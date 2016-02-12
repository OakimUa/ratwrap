package de.zalando.mass.ratwrap.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.zalando.mass.ratwrap.annotation.*;
import de.zalando.mass.ratwrap.enums.RequestMethod;
import de.zalando.mass.ratwrap.sse.ClosableBlockingQueue;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import lombok.NonNull;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.zalando.problem.Problem;
import ratpack.exec.Blocking;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.http.HttpMethod;
import ratpack.stream.Streams;
import ratpack.stream.TransformablePublisher;
import ratpack.websocket.WebSocketHandler;
import ratpack.websocket.WebSockets;

import javax.annotation.PostConstruct;
import javax.ws.rs.core.Response;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toMap;
import static ratpack.jackson.Jackson.json;

@ServerRegistry
public class HandlerDispatcher implements Handler {
    private static final Logger LOGGER = LoggerFactory.getLogger(HandlerDispatcher.class);

    @Autowired
    private ApplicationContext context;
    private Map<HttpMethod, Map<Pattern, ControllerHandler>> handlers;

    // TODO: do with BeanPostProcessor
    @PostConstruct
    public void init() {
        Map<HttpMethod, Map<String, ControllerHandler>> handlers = new HashMap<>();
        final Map<String, Object> controllers = context.getBeansWithAnnotation(RequestController.class);
        controllers.values().stream().forEach(controller -> Arrays.asList(controller.getClass().getMethods()).stream()
                .filter(method -> method.isAnnotationPresent(RequestHandler.class))
                .forEach(method -> {
                    final RequestController requestControllerAnnotation = controller.getClass().getAnnotation(RequestController.class);
                    final RequestHandler handlerAnnotation = method.getAnnotation(RequestHandler.class);
                    final String targetPath = requestControllerAnnotation.uri() + handlerAnnotation.uri();
                    final RequestMethod requestMethod = handlerAnnotation.method();
                    final HttpMethod key = requestMethod.toHttpMethod();
                    if (!handlers.containsKey(key)) {
                        handlers.put(key, new HashMap<>());
                    }
                    final String targetRegexp = HandlerUtils.toRegexp(targetPath);
                    LOGGER.debug("Handler found: [" + controller.getClass().getSimpleName() + "." + method.getName() + "] " + requestMethod.name() + " " + targetPath);
                    if (handlers.get(key).containsKey(targetRegexp)) {
                        throw new RuntimeException("Duplicate path: " + targetPath);
                    }
                    handlers.get(key).put(targetRegexp, new ControllerHandler(requestControllerAnnotation, handlerAnnotation, controller, method));
                }));
        this.handlers = handlers.entrySet().stream()
                .collect(toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().entrySet().stream()
                                .collect(toMap(
                                        entry1 -> Pattern.compile(entry1.getKey()),
                                        Map.Entry::getValue))));
    }

    @Override
    public void handle(Context ctx) throws Exception {
        final Optional<ControllerHandler> handler = findHandler(ctx);
        Class<?> bodyType = null;
        int bodyIndex = -1;

        if (handler.isPresent()) {
            final ControllerHandler h = handler.get();
            LOGGER.debug(ctx.getRequest().getMethod() + " " + ctx.getRequest().getRawUri() + " -> " + h.getController().getClass().getSimpleName() + "." + h.getMethod().getName());
            final Parameter[] parameters = h.getMethod().getParameters();
            final Object[] values = new Object[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                if (parameters[i].isAnnotationPresent(PathVariable.class)) {
                    values[i] = HandlerUtils.extractPathVariable(parameters[i], h.getTargetPath(), ctx.getRequest().getPath());
                } else if (parameters[i].isAnnotationPresent(QueryParam.class)) {
                    values[i] = HandlerUtils.extractQueryParam(parameters[i], ctx.getRequest().getQueryParams());
                } else if (parameters[i].isAnnotationPresent(HeaderParam.class)) {
                    values[i] = HandlerUtils.extractHeaderParam(parameters[i], ctx.getRequest().getHeaders());
                } else if (parameters[i].isAnnotationPresent(ContextParam.class)) {
                    values[i] = HandlerUtils.extractContextParam(parameters[i], ctx);
                } else {
                    bodyType = parameters[i].getType();
                    bodyIndex = i;
                }
            }
            if (bodyIndex >= 0) {
                final int finalBodyIndex = bodyIndex;
                ctx.parse(bodyType).then(body -> {
                    values[finalBodyIndex] = body;
                    ctx.insert(ctx1 -> callHandler(h, values, ctx1));
                });
            } else {
                ctx.insert(ctx1 -> callHandler(h, values, ctx1));
            }
        } else {
            LOGGER.debug(ctx.getRequest().getMethod() + " " + ctx.getRequest().getRawUri() + " -> NEXT [handler not found]");
            ctx.next();
        }
    }

    private void callHandler(ControllerHandler h, Object[] values, Context ctx) {
        if (h.getHandlerDef().blocking()) {
            Blocking.get(() -> {
                try {
                    return h.getMethod().invoke(h.getController(), values);
                } catch (Exception e) {
                    LOGGER.warn(e.getCause().getMessage(), e.getCause());
                    if (e.getCause() instanceof Problem) {
                        return e.getCause();
                    } else {
                        return Problem.valueOf(Response.Status.INTERNAL_SERVER_ERROR, e.getCause().getMessage());
                    }
                }
            }).then(result -> doResponse(h, ctx, result));
        } else {
            Object result;
            try {
                result = h.getMethod().invoke(h.getController(), values);
            } catch (Exception e) {
                LOGGER.error(e.getCause().getMessage(), e.getCause());
                if (e.getCause() instanceof Problem) {
                    result = e.getCause();
                } else {
                    result = Problem.valueOf(Response.Status.INTERNAL_SERVER_ERROR, e.getCause().getMessage());
                }
            }
            doResponse(h, ctx, result);
        }
    }

    private void doResponse(ControllerHandler h, Context ctx, Object result) {
        final int defStatus = h.getHandlerDef().status();
        if (result instanceof Problem) {
            Problem problem = (Problem) result;
            ctx.getResponse().status(problem.getStatus().getStatusCode());
            ctx.getResponse().contentType("application/problem+json");
            ctx.render(json(problem));
        } else if (result instanceof ResponseEntity) {
            ResponseEntity responseEntity = (ResponseEntity) result;
            ctx.getResponse().status(responseEntity.getStatusCode().value());
            responseEntity.getHeaders().toSingleValueMap().entrySet().stream()
                    .forEach(entry -> ctx.getResponse().getHeaders().add(entry.getKey(), entry.getValue()));
            render(h, ctx, responseEntity.getBody());
        } else if (result instanceof WebSocketHandler) {
            WebSocketHandler webSocketHandler = (WebSocketHandler) result;
            WebSockets.websocket(ctx, webSocketHandler);
        } else if (result instanceof ClosableBlockingQueue) {
            ClosableBlockingQueue<?> queue = (ClosableBlockingQueue) result;
            switch (h.getHandlerDef().longResponseType()) {
                case WEBSOCKET_BROADCAST:
                    WebSockets.websocketBroadcast(ctx,
                            Streams.yield(yieldRequest -> queue.maybeTake().orElse(null))
                                    .map(o -> ctx.get(ObjectMapper.class).writeValueAsString(o)));
                    break;
                case JSON_LONG_POLLING:
                    final TransformablePublisher<ByteBuf> publisher = Streams.yield(yieldRequest -> queue.maybeTake().orElse(null))
                            .map(o -> ctx.get(ObjectMapper.class).writeValueAsString(o))
                            .map(data -> HandlerUtils.transformStringToByteBuf(data, ctx.get(ByteBufAllocator.class)));
                    ctx.getResponse().contentType(h.getHandlerDef().produce());
                    ctx.getResponse().sendStream(publisher);
                    break;
                case SERVER_SENT_EVENTS:
                default:
                    final TransformablePublisher<Object> stream = Streams.yield(yieldRequest -> queue.maybeTake().orElse(null));
                    HandlerUtils.toSSE(h, ctx, stream);
                    break;
            }
        } else if (result instanceof Publisher) {
            Publisher<?> stream = (Publisher) result;
            switch (h.getHandlerDef().longResponseType()) {
                case WEBSOCKET_BROADCAST:
                    WebSockets.websocketBroadcast(ctx,
                            Streams.transformable(stream)
                                    .map(o -> ctx.get(ObjectMapper.class).writeValueAsString(o)));
                    break;
                case JSON_LONG_POLLING:
                    final TransformablePublisher<ByteBuf> publisher = Streams.transformable(stream)
                            .map(o -> ctx.get(ObjectMapper.class).writeValueAsString(o))
                            .map(data -> HandlerUtils.transformStringToByteBuf(data, ctx.get(ByteBufAllocator.class)));
                    ctx.getResponse().contentType(h.getHandlerDef().produce());
                    ctx.getResponse().sendStream(publisher);
                    break;
                case SERVER_SENT_EVENTS:
                default:
                    HandlerUtils.toSSE(h, ctx, stream);
                    break;
            }
        } else if (result instanceof Throwable) {
            doResponse(h, ctx, Problem.valueOf(Response.Status.INTERNAL_SERVER_ERROR, ((Throwable) result).getMessage()));
        } else {
            ctx.getResponse().contentType(h.getHandlerDef().produce());
            ctx.getResponse().status(defStatus);
            render(h, ctx, result);
        }
    }

    // todo: add content type support
    private void render(ControllerHandler h, Context ctx, Object body) {
        final String contentType = h.getHandlerDef().produce();
        if (contentType.startsWith("text")) {
            ctx.render(body.toString());
        } else if (contentType.endsWith("json")) {
            ctx.render(json(body));
        } else {
            ctx.render(body.toString());
        }
    }

    @NonNull
    private Optional<ControllerHandler> findHandler(@NonNull final Context ctx) {
        final HttpMethod method = ctx.getRequest().getMethod();
        if (!handlers.containsKey(method)) {
            return Optional.empty();
        }
        return handlers.get(method).keySet().stream()
                .filter(pattern -> pattern.matcher(ctx.getRequest().getPath()).matches())
                .findFirst()
                .map(pattern -> handlers.get(method).get(pattern));
    }
}

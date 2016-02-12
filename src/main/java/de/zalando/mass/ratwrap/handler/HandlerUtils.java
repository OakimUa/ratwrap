package de.zalando.mass.ratwrap.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import de.zalando.mass.ratwrap.annotation.HeaderParam;
import de.zalando.mass.ratwrap.annotation.PathVariable;
import de.zalando.mass.ratwrap.annotation.QueryParam;
import de.zalando.mass.ratwrap.annotation.SSEParams;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.util.CharsetUtil;
import lombok.NonNull;
import org.reactivestreams.Publisher;
import ratpack.handling.Context;
import ratpack.http.Headers;
import ratpack.sse.ServerSentEvents;
import ratpack.util.MultiValueMap;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Optional;

import static io.netty.util.CharsetUtil.UTF_8;
import static ratpack.sse.ServerSentEvents.serverSentEvents;

class HandlerUtils {
    @NonNull
    static String toRegexp(@NonNull final String pathDef) {
        if (!pathDef.contains("{"))
            return pathDef;
        final String s = pathDef.replaceAll("\\}\\{", "");
        final String[] split = s.split("(\\{|\\})");
        int seed = s.startsWith("{") ? 1 : 0;
        for (int i = seed; i < split.length; i++) {
            if (i % 2 != 0) {
                split[i] = "[a-zA-Z0-9_-]*";
            }
        }
        return String.join("", split);
    }

    static Object toSimpleObject(String res, Class<?> targetType) {
        if (targetType.equals(Integer.class)) {
            return Integer.parseInt(res);
        } else if (targetType.equals(Long.class)) {
            return Long.parseLong(res);
        } else {
            return res;
        }
    }

    @NonNull
    static Object extractPathVariable(@NonNull final Parameter parameter, @NonNull final String pathDef, @NonNull final String path) {
        final String[] split = pathDef.split("\\{" + parameter.getAnnotation(PathVariable.class).value() + "\\}");
        String res = path;
        for (String aSplit : split) {
            res = res.replaceFirst(toRegexp(aSplit), "");
        }
        return toSimpleObject(res, parameter.getType());
    }

    static Object extractQueryParam(@NonNull final Parameter parameter, @NonNull final MultiValueMap<String, String> queryParams) {
        final QueryParam annotation = parameter.getAnnotation(QueryParam.class);
        final String name = annotation.value();
        if (!queryParams.containsKey(name)) {
            return null;
        }
        return toSimpleObject(queryParams.get(name), parameter.getType());
    }

    static Object extractHeaderParam(@NonNull final Parameter parameter, @NonNull final Headers headers) {
        final HeaderParam annotation = parameter.getAnnotation(HeaderParam.class);
        final String name = annotation.value();
        if (!headers.contains(name)) {
            return null;
        }
        return toSimpleObject(headers.get(name), parameter.getType());
    }

    static Object extractContextParam(@NonNull final Parameter parameter, @NonNull final Context ctx) {
        return ctx.maybeGet(parameter.getType()).orElse(null);
    }

    @NonNull
    public static ByteBuf transformStringToByteBuf(@NonNull final String data, @NonNull final ByteBufAllocator bufferAllocator) throws IOException {
        ByteBuf buffer = bufferAllocator.buffer();
        OutputStream outputStream = new ByteBufOutputStream(buffer);
        Writer writer = new OutputStreamWriter(outputStream, CharsetUtil.getEncoder(UTF_8));
        writer.append(data).append("\n").flush();
        writer.close();
        return buffer;
    }

    public static void toSSE(@NonNull final ControllerHandler h, @NonNull final Context ctx, @NonNull final Publisher<?> stream) {
        final String eventName = Optional.ofNullable(h.getMethod().getAnnotation(SSEParams.class))
                .map(sseParams -> Strings.isNullOrEmpty(sseParams.eventName()) ? h.getMethod().getName() : sseParams.eventName())
                .orElse(h.getMethod().getName());
        final String eventIdMethod = Optional.ofNullable(h.getMethod().getAnnotation(SSEParams.class))
                .map(SSEParams::eventIdMethod)
                .orElse("toString");
        ServerSentEvents events = serverSentEvents(stream, e -> e
                .id(o -> getEventId(o, eventIdMethod))
                .event(eventName)
                .data(i -> ctx.get(ObjectMapper.class).writeValueAsString(i)));
        ctx.render(events);
    }

    @NonNull
    public static String getEventId(@NonNull final Object o, @NonNull final String methodName) {
        try {
            final Method method = o.getClass().getMethod(methodName);
            return method.invoke(o).toString();
        } catch (Exception e) {
            return o.toString();
        }
    }
}

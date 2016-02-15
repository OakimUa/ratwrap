package de.zalando.mass.ratwrap.handler;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import de.zalando.mass.ratwrap.annotation.FilterUri;
import de.zalando.mass.ratwrap.annotation.ServerRegistry;
import de.zalando.mass.ratwrap.filter.RequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.zalando.problem.ThrowableProblem;
import ratpack.func.Pair;
import ratpack.handling.Context;
import ratpack.handling.Handler;

import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static ratpack.jackson.Jackson.json;

@ServerRegistry
public class FilterDispatcher implements Handler, BeanPostProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(FilterDispatcher.class);

    private Multimap<String, RequestFilter> filters = HashMultimap.create();

    @Override
    public void handle(Context ctx) throws Exception {
        final String logReq = ctx.getRequest().getMethod() + " " + ctx.getRequest().getRawUri() + " -> ";
        final String path = ctx.getRequest().getUri();
        final List<RequestFilter> filters = this.filters.asMap().entrySet().stream()
                .filter(entry -> path.startsWith(entry.getKey()))
                .flatMap(entry -> entry.getValue().stream().map(requestFilter -> Pair.of(entry.getKey(), requestFilter)))
                .sorted((p1, p2) -> p1.getLeft().compareTo(p2.getLeft()))
                .map(pair -> {
                    LOGGER.debug(logReq + "[" + pair.getRight().getClass().getSimpleName() + "] " + pair.getLeft());
                    return pair.getRight();
                })
                .collect(toList());
//        LOGGER.debug(logReq + filters.stream().map(f -> f.getClass().getSimpleName()).toString());
        if (filters.size() > 0) {
            try {
                ctx.insert(filters.toArray(new RequestFilter[filters.size()]));
            } catch (ThrowableProblem problem) {
                ctx.getResponse().status(problem.getStatus().getStatusCode());
                ctx.getResponse().contentType("application/problem+json");
                ctx.render(json(problem));
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            ctx.next();
        }
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (RequestFilter.class.isAssignableFrom(bean.getClass())) {
            RequestFilter filter = (RequestFilter) bean;
            LOGGER.debug("Filter found: " + bean.getClass().getSimpleName());
            String uri = "/";
            try {
                uri = Optional.ofNullable(filter.getClass().getMethod("handle", Context.class).getAnnotation(FilterUri.class))
                        .map(FilterUri::value).orElse("/");
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
            if (!uri.startsWith("/")) {
                uri = "/" + uri;
            }
            if (!uri.endsWith("/")) {
                uri = uri + "/";
            }
            filters.put(uri, filter);
        }
        return bean;
    }
}

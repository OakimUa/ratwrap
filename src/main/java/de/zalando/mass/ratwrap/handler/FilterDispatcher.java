package de.zalando.mass.ratwrap.handler;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import de.zalando.mass.ratwrap.annotation.FilterUri;
import de.zalando.mass.ratwrap.annotation.ServerRegistry;
import de.zalando.mass.ratwrap.filter.RequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.zalando.problem.ThrowableProblem;
import ratpack.func.Pair;
import ratpack.handling.Context;
import ratpack.handling.Handler;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toList;
import static ratpack.jackson.Jackson.json;

@ServerRegistry
public class FilterDispatcher implements Handler, BeanPostProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(FilterDispatcher.class);

    @Autowired
    private ApplicationContext context;

    private Multimap<Pattern, RequestFilter> filters = HashMultimap.create();

    @Override
    public void handle(Context ctx) throws Exception {
        final String logReq = ctx.getRequest().getMethod() + " " + ctx.getRequest().getRawUri() + " -> ";
        final String path = ctx.getRequest().getUri();
        final List<RequestFilter> filters = this.filters.asMap().entrySet().stream()
                .filter(entry -> entry.getKey().matcher(path).matches())
                .flatMap(entry -> entry.getValue().stream().map(requestFilter -> Pair.of(entry.getKey(), requestFilter)))
                .sorted((p1, p2) -> p1.getLeft().pattern().compareTo(p2.getLeft().pattern()))
                .map(pair -> {
                    LOGGER.debug(logReq + "[" + pair.getRight().getClass().getSimpleName() + "] " + pair.getLeft());
                    return pair.getRight();
                })
                .collect(toList());
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
            try {
                final String uri = Optional.ofNullable(filter.getClass().getMethod("handle", Context.class).getAnnotation(FilterUri.class))
                        .map(FilterUri::value).orElse("/**");
                addFilter(uri, filter);
            } catch (NoSuchMethodException e) {
                LOGGER.warn(e.getMessage(), e);
            }
        }
        return bean;
    }

    public void addFilter(String uri, RequestFilter filter) {
        if (filter != null) {
            if (!uri.startsWith("/")) {
                uri = "/" + uri;
            }
            if (uri.endsWith("/")) {
                uri = uri.substring(0, uri.length() - 1);
            }
            LOGGER.debug("Filter registration: [" + filter.getClass().getSimpleName() + "] " + uri);
            filters.put(Pattern.compile(HandlerUtils.toRegexp(uri)), filter);
        }
    }

    public void addFilter(String uri, Class<? extends RequestFilter> filterClass) {
        RequestFilter filter = null;
        try {
            filter = context.getBean(filterClass);
        } catch (BeansException be) {
            LOGGER.debug("Bean filter for class [" + filterClass.getName() + "] is missing, trying to instantiate filter.");
            try {
                final RequestFilter filterInstance = filterClass.newInstance();
                ((AnnotationConfigApplicationContext) context).getBeanFactory().autowireBean(filterInstance);
                ((AnnotationConfigApplicationContext) context).getBeanFactory().registerSingleton(filterClass.getSimpleName(), filterInstance);
                filter = context.getBean(filterClass);
            } catch (InstantiationException | IllegalAccessException e) {
                LOGGER.warn(e.getMessage(), e);
            }
        }
        addFilter(uri, filter);
    }
}

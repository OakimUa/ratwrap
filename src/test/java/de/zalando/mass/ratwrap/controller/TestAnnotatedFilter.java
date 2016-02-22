package de.zalando.mass.ratwrap.controller;

import de.zalando.mass.ratwrap.annotation.FilterUri;
import de.zalando.mass.ratwrap.filter.RequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ratpack.handling.Context;

@Component
public class TestAnnotatedFilter implements RequestFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestAnnotatedFilter.class);

    @Override
    @FilterUri("/some-not-existing-url/**")
    public void handle(Context ctx) {
        LOGGER.debug("TestAnnotatedFilter");
        ctx.getResponse().getHeaders().add("x-annotated-filter", "done");
        ctx.next();
    }
}

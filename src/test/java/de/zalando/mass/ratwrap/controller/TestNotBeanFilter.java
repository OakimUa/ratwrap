package de.zalando.mass.ratwrap.controller;

import de.zalando.mass.ratwrap.filter.RequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.handling.Context;

public class TestNotBeanFilter implements RequestFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestNotBeanFilter.class);

    @Override
    public void handle(Context ctx) {
        LOGGER.debug("TestNotBeanFilter");
        ctx.getResponse().getHeaders().add("x-not-bean-filter", "done");
        ctx.next();
    }
}

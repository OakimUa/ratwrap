package de.zalando.mass.ratwrap.controller;

import de.zalando.mass.ratwrap.annotation.FilterUri;
import de.zalando.mass.ratwrap.filter.RequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.zalando.problem.Problem;
import ratpack.handling.Context;

import javax.ws.rs.core.Response;

@Component
public class TestRequestFilter implements RequestFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestRequestFilter.class);

    @Override
    @FilterUri("filtered")
    public void handle(Context ctx) {
        LOGGER.debug("TestRequestFilter");
        ctx.getResponse().getHeaders().add("x-request-filter", "done");
        if ("block".equals(ctx.getRequest().getHeaders().get("x-block-in-request-filter"))) {
            throw Problem.valueOf(Response.Status.FORBIDDEN);
        } else {
            ctx.next();
        }
    }
}

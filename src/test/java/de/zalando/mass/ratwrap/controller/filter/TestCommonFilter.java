package de.zalando.mass.ratwrap.controller.filter;

import de.zalando.mass.ratwrap.filter.RequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.zalando.problem.Problem;
import ratpack.handling.Context;

import javax.ws.rs.core.Response;

@Component
public class TestCommonFilter implements RequestFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestCommonFilter.class);

    @Override
    public void handle(Context ctx) {
        LOGGER.debug("TestCommonFilter");
        ctx.getResponse().getHeaders().add("x-common-filter", "done");
        if ("block".equals(ctx.getRequest().getHeaders().get("x-block-in-common-filter"))) {
            throw Problem.valueOf(Response.Status.FORBIDDEN);
        } else {
            ctx.next();
        }
    }
}

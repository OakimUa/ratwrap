package de.zalando.mass.ratwrap.handler;

import de.zalando.mass.ratwrap.annotation.ServerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zalando.problem.Problem;
import ratpack.error.ServerErrorHandler;
import ratpack.handling.Context;

import javax.ws.rs.core.Response;

import static ratpack.jackson.Jackson.json;

@ServerRegistry
public class ProblemHandler implements ServerErrorHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProblemHandler.class);
    @Override
    public void error(Context ctx, Throwable throwable) throws Exception {
        LOGGER.warn(throwable.getMessage(), throwable);
        Problem problem;
        if (throwable instanceof Problem) {
            problem = (Problem) throwable;
        } else if (throwable.getCause() instanceof Problem) {
            problem = (Problem) throwable.getCause();
        } else {
            problem = Problem.valueOf(Response.Status.INTERNAL_SERVER_ERROR, throwable.getMessage());
        }
        ctx.getResponse().status(problem.getStatus().getStatusCode());
        ctx.getResponse().contentType("application/problem+json");
        ctx.render(json(problem));
    }
}

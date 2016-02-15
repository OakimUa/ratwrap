package de.zalando.mass.ratwrap.handler;

import de.zalando.mass.ratwrap.annotation.ServerRegistry;
import org.springframework.stereotype.Component;
import org.zalando.problem.Problem;
import ratpack.error.ServerErrorHandler;
import ratpack.handling.Context;

import javax.ws.rs.core.Response;

import static ratpack.jackson.Jackson.json;

@ServerRegistry
@Component
public class ProblemHandler implements ServerErrorHandler {
    @Override
    public void error(Context ctx, Throwable throwable) throws Exception {
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

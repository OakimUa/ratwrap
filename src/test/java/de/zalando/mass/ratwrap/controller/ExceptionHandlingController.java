package de.zalando.mass.ratwrap.controller;

import de.zalando.mass.ratwrap.annotation.RequestController;
import de.zalando.mass.ratwrap.annotation.RequestHandler;
import de.zalando.mass.ratwrap.enums.RequestMethod;
import org.springframework.http.ResponseEntity;
import org.zalando.problem.Problem;

import static javax.ws.rs.core.Response.Status;

@RequestController(uri = "errors/")
public class ExceptionHandlingController {

    // Wrong!
    @RequestHandler(method = RequestMethod.GET, uri = "wrong/404")
    public ResponseEntity doWrongNotFound() {
        return ResponseEntity.notFound().build();
    }

    // Correct
    @RequestHandler(method = RequestMethod.GET, uri = "404")
    public ResponseEntity doCorrect() throws Exception {
        throw Problem.valueOf(Status.NOT_FOUND);
    }

    // Enforce 500
    @RequestHandler(method = RequestMethod.GET, uri = "500")
    public ResponseEntity doServerError() throws Exception {
        throw new Exception("Error");
    }
}

package de.zalando.mass.ratwrap.controller;

import de.zalando.mass.ratwrap.annotation.RequestController;
import de.zalando.mass.ratwrap.annotation.RequestHandler;

@RequestController(uri = "filtered/")
public class FilteredController {

    @RequestHandler(uri = "test", produce = "text/plain")
    public String test() {
        return "OK";
    }
}

package de.zalando.mass.ratwrap.controller;

import de.zalando.mass.ratwrap.annotation.Filtered;
import de.zalando.mass.ratwrap.annotation.RequestController;
import de.zalando.mass.ratwrap.annotation.RequestHandler;

@RequestController(uri = "filtered/")
@Filtered(TestClassAnnotatedFilter.class)
public class FilteredController {

    @RequestHandler(uri = "test", produce = "text/plain")
    public String test() {
        return "OK";
    }

    @RequestHandler(uri = "annotated-test", produce = "text/plain")
    @Filtered({TestAnnotatedFilter.class, TestNotBeanFilter.class})
    public String annotatedTest() {
        return "OK";
    }
}

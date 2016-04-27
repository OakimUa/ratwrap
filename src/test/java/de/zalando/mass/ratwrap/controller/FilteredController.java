package de.zalando.mass.ratwrap.controller;

import de.zalando.mass.ratwrap.annotation.Filtered;
import de.zalando.mass.ratwrap.annotation.RequestController;
import de.zalando.mass.ratwrap.annotation.RequestHandler;
import de.zalando.mass.ratwrap.controller.filter.TestAnnotatedFilter;
import de.zalando.mass.ratwrap.controller.filter.TestClassAnnotatedFilter;
import de.zalando.mass.ratwrap.controller.filter.TestNotBeanFilter;

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

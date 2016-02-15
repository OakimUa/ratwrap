package de.zalando.mass.ratwrap.annotation;

import org.springframework.stereotype.Controller;

import java.lang.annotation.*;

/**
 * All classes annotated with RequestController will be inspected for request handlers
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Controller
public @interface RequestController {
    /**
     * Defines base URI for all handlers inside controller
     * @return
     */
    String uri() default "";
}

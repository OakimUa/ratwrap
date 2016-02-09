package de.zalando.mass.ratwrap.annotation;

import org.springframework.stereotype.Controller;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Controller
public @interface RequestController {
    String uri() default "";
}

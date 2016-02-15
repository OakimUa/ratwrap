package de.zalando.mass.ratwrap.config;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Enables ratpack via adding configuration to the project
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({HandlerDispatcherConfig.class})
public @interface EnableRatpack {
}

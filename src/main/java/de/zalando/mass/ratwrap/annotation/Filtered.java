package de.zalando.mass.ratwrap.annotation;

import de.zalando.mass.ratwrap.filter.RequestFilter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Filtered {
    Class<? extends RequestFilter>[] value();
}

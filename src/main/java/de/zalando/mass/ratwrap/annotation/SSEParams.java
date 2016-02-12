package de.zalando.mass.ratwrap.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * streaming parameters
 * Used only with longResponseType=LongResponseType.SERVER_SENT_EVENTS
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SSEParams {
    /**
     * event name for event streaming
     * @return
     */
    String eventName() default "";

    /**
     * Method in returned data to calculate event id
     * @return
     */
    String eventIdMethod() default "toString";
}

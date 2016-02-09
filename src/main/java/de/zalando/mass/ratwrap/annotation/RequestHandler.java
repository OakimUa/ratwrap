package de.zalando.mass.ratwrap.annotation;

import de.zalando.mass.ratwrap.enums.RequestMethod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RequestHandler {
    RequestMethod method() default RequestMethod.GET;
    String uri() default "";
    int status() default 200;
    String produce() default "application/json";

    /**
     * Is a blocking operation
     * @return
     */
    boolean blocking() default true;

    /**
     * event name for event streaming
     * @return
     */
    String eventName() default "";

    /**
     * Method in returned data o calculate event id
     * @return
     */
    String eventIdMethod() default "toString";

    /**
     * should open WebSocket connection instead SSE
     * @return
     */
    boolean webSocketBroadcasting() default false;
}

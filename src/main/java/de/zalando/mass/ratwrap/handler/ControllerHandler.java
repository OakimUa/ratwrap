package de.zalando.mass.ratwrap.handler;

import de.zalando.mass.ratwrap.annotation.RequestHandler;
import de.zalando.mass.ratwrap.annotation.RequestController;
import lombok.Data;
import lombok.NonNull;

import java.lang.reflect.Method;

@Data
class ControllerHandler {
    @NonNull
    private final RequestController controllerDef;
    @NonNull
    private final RequestHandler handlerDef;
    @NonNull
    private final Object controller;
    @NonNull
    private final Method method;

    public String getTargetPath() {
        return controllerDef.uri() + handlerDef.uri();
    }
}

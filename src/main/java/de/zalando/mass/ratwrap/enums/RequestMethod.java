package de.zalando.mass.ratwrap.enums;

import io.netty.handler.codec.http.HttpMethod;
import lombok.NonNull;
import ratpack.http.internal.DefaultHttpMethod;

public enum RequestMethod {

    GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS, TRACE;

    @NonNull
    public ratpack.http.HttpMethod toHttpMethod() {
        return toHttpMethod(this);
    }

    @NonNull
	public static ratpack.http.HttpMethod toHttpMethod(@NonNull final RequestMethod requestMethod) {
        return DefaultHttpMethod.valueOf(new HttpMethod(requestMethod.name()));
    }
}

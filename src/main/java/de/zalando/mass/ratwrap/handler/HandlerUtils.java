package de.zalando.mass.ratwrap.handler;

import de.zalando.mass.ratwrap.annotation.HeaderParam;
import de.zalando.mass.ratwrap.annotation.PathVariable;
import de.zalando.mass.ratwrap.annotation.QueryParam;
import lombok.NonNull;
import ratpack.handling.Context;
import ratpack.http.Headers;
import ratpack.util.MultiValueMap;

import java.lang.reflect.Parameter;

class HandlerUtils {
    @NonNull
    static String toRegexp(@NonNull final String pathDef) {
        if (!pathDef.contains("{"))
            return pathDef;
        final String s = pathDef.replaceAll("\\}\\{", "");
        final String[] split = s.split("(\\{|\\})");
        int seed = s.startsWith("{") ? 1 : 0;
        for (int i = seed; i < split.length; i++) {
            if (i % 2 != 0) {
                split[i] = "[a-zA-Z0-9_-]*";
            }
        }
        return String.join("", split);
    }

    static Object toSimpleObject(String res, Class<?> targetType) {
        if (targetType.equals(Integer.class)) {
            return Integer.parseInt(res);
        } else if (targetType.equals(Long.class)) {
            return Long.parseLong(res);
        } else {
            return res;
        }
    }

    @NonNull
    static Object extractPathVariable(@NonNull final Parameter parameter, @NonNull final String pathDef, @NonNull final String path) {
        final String[] split = pathDef.split("\\{" + parameter.getAnnotation(PathVariable.class).value() + "\\}");
        String res = path;
        for (String aSplit : split) {
            res = res.replaceFirst(toRegexp(aSplit), "");
        }
        return toSimpleObject(res, parameter.getType());
    }

    static Object extractQueryParam(@NonNull final Parameter parameter, @NonNull final MultiValueMap<String, String> queryParams) {
        final QueryParam annotation = parameter.getAnnotation(QueryParam.class);
        final String name = annotation.value();
        if (!queryParams.containsKey(name)) {
            return null;
        }
        return toSimpleObject(queryParams.get(name), parameter.getType());
    }

    static Object extractHeaderParam(@NonNull final Parameter parameter, @NonNull final Headers headers) {
        final HeaderParam annotation = parameter.getAnnotation(HeaderParam.class);
        final String name = annotation.value();
        if (!headers.contains(name)) {
            return null;
        }
        return toSimpleObject(headers.get(name), parameter.getType());
    }

    static Object extractContextParam(@NonNull final Parameter parameter, @NonNull final Context ctx) {
        return ctx.maybeGet(parameter.getType()).orElse(null);
    }
}

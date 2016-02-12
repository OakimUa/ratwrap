package de.zalando.mass.ratwrap.util;

import org.slf4j.Logger;
import ratpack.http.client.ReceivedResponse;

import java.util.Set;

public class TestUtils {
    public static void logResponse(Logger LOGGER, ReceivedResponse response, String path) {
        LOGGER.debug(path);
        LOGGER.debug("|-> Status code: " + response.getStatusCode());
        LOGGER.debug("|-> Headers:");
        final Set<String> names = response.getHeaders().getNames();
        names.stream().forEach(name -> LOGGER.debug("    |-> " + name + " : " + response.getHeaders().get(name)));
        LOGGER.debug("|-> Body: " + response.getBody().getText());
    }
}

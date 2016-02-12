package de.zalando.mass.ratwrap.sse.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;

import java.io.IOException;

public class LPSubscriber<T> extends AbstractLongResponseSubscriber<T> {
    public LPSubscriber(ObjectMapper objectMapper, Class<T> targetClass) {
        super(objectMapper, targetClass);
    }

    @Override
    protected Boolean fetchQueue() {
        try {
            LOGGER.debug("|+++> start fetching queue");
            String dataToken = "";
            while (isRunning() || !lineBuffer.isEmpty()) {
                final String line = lineBuffer.take();
                LOGGER.debug("|==> " + line);
                if (Strings.isNullOrEmpty(line) || line.equals(SEPARATOR_TOKEN) || line.equals(POISON_TOKEN)) {
                    if (!Strings.isNullOrEmpty(dataToken)) {
                        try {
                            final HttpEvent<T> httpEvent = new HttpEvent<>(
                                    null,
                                    null,
                                    null,
                                    objectMapper.readValue(dataToken, targetClass));
                            LOGGER.debug("|==< " + httpEvent.toString());
                            eventBuffer.put(httpEvent);
                            dataToken = "";
                        } catch (IOException e) {
                            e.printStackTrace();
                            return false;
                        }
                    }
                    if (line.equals(POISON_TOKEN)) {
                        eventBuffer.put(new PoisonHttpEvent<>(error));
                    }
                } else {
                    dataToken = dataToken + line.trim();
                }
            }
            LOGGER.debug("|+++< stop fetching queue");
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    protected String eventSeparator() {
        return "\n";
    }
}

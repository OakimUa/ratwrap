package de.zalando.mass.ratwrap.sse.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.primitives.Ints;

import java.io.IOException;

public class SSESubscriber<T> extends AbstractLongResponseSubscriber<T> {
    protected static final String EVENT_TOKEN = "event:";
    protected static final String DATA_TOKEN = "data:";
    protected static final String ID_TOKEN = "id:";
    protected static final String RETRY_TOKEN = "retry:";

    public SSESubscriber(ObjectMapper objectMapper, Class<T> targetClass) {
        super(objectMapper, targetClass);
    }

    @Override
    protected Boolean fetchQueue() {
        try {
            LOGGER.debug("|+++> start fetching queue");
            String idToken = null;
            String eventToken = null;
            Integer retryToken = null;
            String dataToken = "";
            while (isRunning() || !lineBuffer.isEmpty()) {
                final String line = lineBuffer.take();
                LOGGER.debug("|==> " + line);
                if (Strings.isNullOrEmpty(line) || line.equals(SEPARATOR_TOKEN) || line.equals(POISON_TOKEN)) {
                    if (!Strings.isNullOrEmpty(dataToken)) {
                        try {
                            final HttpEvent<T> httpEvent = new HttpEvent<>(
                                    idToken,
                                    eventToken,
                                    retryToken,
                                    objectMapper.readValue(dataToken, targetClass));
                            LOGGER.debug("|==< " + httpEvent.toString());
                            eventBuffer.put(httpEvent);
                            idToken = null;
                            eventToken = null;
                            retryToken = null;
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
                    if (line.startsWith(ID_TOKEN)) {
                        idToken = line.substring(ID_TOKEN.length()).trim();
                    } else if (line.startsWith(EVENT_TOKEN)) {
                        eventToken = line.substring(EVENT_TOKEN.length()).trim();
                    } else if (line.startsWith(RETRY_TOKEN)) {
                        retryToken = Ints.tryParse(line.substring(RETRY_TOKEN.length()).trim());
                    } else if (line.startsWith(DATA_TOKEN)) {
                        dataToken = dataToken + line.substring(DATA_TOKEN.length()).trim();
                    }
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
        return "\n\n";
    }
}

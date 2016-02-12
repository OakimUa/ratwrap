package de.zalando.mass.ratwrap.controller;

import de.zalando.mass.ratwrap.annotation.RequestController;
import de.zalando.mass.ratwrap.annotation.RequestHandler;
import de.zalando.mass.ratwrap.data.InputData;
import de.zalando.mass.ratwrap.enums.LongResponseType;
import de.zalando.mass.ratwrap.sse.ClosableBlockingQueue;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static de.zalando.mass.ratwrap.enums.RequestMethod.GET;

@RequestController(uri = "polling/")
public class JsonStreamController {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonStreamController.class);

    @RequestHandler(method = GET, uri = "regular", longResponseType = LongResponseType.JSON_LONG_POLLING)
    public Publisher<InputData> regular() {
        return Utils.testPublisher(-1, LOGGER);
    }

    @RequestHandler(method = GET, uri = "queued", longResponseType = LongResponseType.JSON_LONG_POLLING)
    public ClosableBlockingQueue<InputData> queued() {
        return Utils.testQueue(-1);
    }
}

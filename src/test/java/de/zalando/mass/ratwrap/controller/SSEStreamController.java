package de.zalando.mass.ratwrap.controller;

import com.google.common.primitives.Ints;
import de.zalando.mass.ratwrap.annotation.HeaderParam;
import de.zalando.mass.ratwrap.annotation.RequestController;
import de.zalando.mass.ratwrap.annotation.RequestHandler;
import de.zalando.mass.ratwrap.annotation.SSEParams;
import de.zalando.mass.ratwrap.data.InputData;
import de.zalando.mass.ratwrap.enums.LongResponseType;
import de.zalando.mass.ratwrap.sse.ClosableBlockingQueue;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static de.zalando.mass.ratwrap.enums.RequestMethod.GET;

@RequestController(uri = "streams/")
public class SSEStreamController {
    private static final Logger LOGGER = LoggerFactory.getLogger(SSEStreamController.class);

    @RequestHandler(method = GET, uri = "regular", longResponseType = LongResponseType.SERVER_SENT_EVENTS)
    @SSEParams(eventName = "scheduled_event", eventIdMethod = "getField2")
    public Publisher<InputData> regular(
            @HeaderParam("Last-Event-ID") final String lastEventID) {
        int start = Ints.tryParse(Optional.ofNullable(lastEventID).orElse("-1"));
        return Utils.testPublisher(start, LOGGER);
    }

    @RequestHandler(method = GET, uri = "queued", longResponseType = LongResponseType.SERVER_SENT_EVENTS)
    @SSEParams(eventName = "queued_event", eventIdMethod = "getField2")
    public ClosableBlockingQueue<InputData> queued(
            @HeaderParam("Last-Event-ID") final String lastEventID) {
        int start = Ints.tryParse(Optional.ofNullable(lastEventID).orElse("-1"));
        return Utils.testQueue(start);
    }
}

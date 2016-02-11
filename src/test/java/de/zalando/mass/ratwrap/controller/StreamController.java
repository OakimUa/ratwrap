package de.zalando.mass.ratwrap.controller;

import com.google.common.primitives.Ints;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.zalando.mass.ratwrap.annotation.HeaderParam;
import de.zalando.mass.ratwrap.annotation.RequestController;
import de.zalando.mass.ratwrap.annotation.RequestHandler;
import de.zalando.mass.ratwrap.data.InputData;
import de.zalando.mass.ratwrap.sse.ClosableBlockingQueue;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ratpack.stream.Streams;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static de.zalando.mass.ratwrap.enums.RequestMethod.GET;

@RequestController(uri = "streams/")
public class StreamController {
    private static final Logger LOGGER = LoggerFactory.getLogger(StreamController.class);

    @RequestHandler(method = GET, uri = "regular", eventName = "scheduled_event", eventIdMethod = "getField2")
    public Publisher<InputData> regular(
            @HeaderParam("Last-Event-ID") final String lastEventID) {
        int start = Ints.tryParse(Optional.ofNullable(lastEventID).orElse("-1"));
        return Streams.yield(yieldRequest -> {
            if (yieldRequest.getRequestNum() > 9)
                return null;
//            TimeUnit.SECONDS.sleep(2);
            LOGGER.debug("server -> " + yieldRequest.toString());
            return new InputData("" + yieldRequest.getRequestNum() + ":" + yieldRequest.getSubscriberNum(),
                    (int) yieldRequest.getRequestNum() + start + 1);
        });
    }

    @RequestHandler(method = GET, uri = "queued", eventName = "queued_event", eventIdMethod = "getField2")
    public ClosableBlockingQueue<InputData> queued(
            @HeaderParam("Last-Event-ID") final String lastEventID) {
        int start = Ints.tryParse(Optional.ofNullable(lastEventID).orElse("-1"));
        final ClosableBlockingQueue<InputData> dataQueue = new ClosableBlockingQueue<>(5);
        final ExecutorService executorService = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
                .setNameFormat("test-queue-supplier-%d")
                .setDaemon(true)
                .build());
        CompletableFuture.supplyAsync(() -> {
            for (int i = 0; i < 10; i++) {
                try {
//                    TimeUnit.SECONDS.sleep(2);
                    dataQueue.put(new InputData("queue", i + start + 1));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            try {
                dataQueue.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }, executorService);
        return dataQueue;
    }
}

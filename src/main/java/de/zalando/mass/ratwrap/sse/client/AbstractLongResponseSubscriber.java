package de.zalando.mass.ratwrap.sse.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

public abstract class AbstractLongResponseSubscriber<T> implements Subscriber<ByteBuf>, EventReceiver<T> {
    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    protected static final int BULK_SIZE = 1;
    protected static final int QUEUE_SIZE = 100;
    protected static final String SEPARATOR_TOKEN = "EVENT_SEPARATOR";
    protected static final String POISON_TOKEN = "END_OF_STREAM";

    protected final ObjectMapper objectMapper;
    protected final Class<T> targetClass;
    protected Subscription subscription;
    protected final BlockingQueue<String> lineBuffer;
    protected final BlockingQueue<HttpEvent<T>> eventBuffer;
    protected boolean running;
    protected Throwable error = null;

    public AbstractLongResponseSubscriber(ObjectMapper objectMapper, Class<T> targetClass) {
        this.objectMapper = objectMapper;
        this.targetClass = targetClass;
        lineBuffer = new ArrayBlockingQueue<>(QUEUE_SIZE);
        eventBuffer = new ArrayBlockingQueue<>(QUEUE_SIZE);
    }

    // ToDo: solve blocking thread where onNext called
    public HttpEvent<T> next() throws InterruptedException {
        return eventBuffer.take();
    }

    protected synchronized boolean isRunning() {
        return running;
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
        subscription.request(BULK_SIZE);
        running = true;

        final ExecutorService executorService = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
                .setNameFormat("event-subscriber-%d")
                .setDaemon(true)
                .build());
        CompletableFuture.supplyAsync(this::fetchQueue, executorService);
    }

    protected abstract Boolean fetchQueue();

    @Override
    public void onNext(ByteBuf data) {
        final ByteBufInputStream bbis = new ByteBufInputStream(data);

        try {
            subscription.request(BULK_SIZE);
            final byte[] bytes = StreamUtils.copyToByteArray(bbis);
            final String event = new String(bytes).replace(eventSeparator(),"\nEVENT_SEPARATOR\n");
            LOGGER.debug("|--> " + event.replace("\n", "\\n"));
            final List<String> lines = Arrays.asList(event.split("\n"));
            lineBuffer.addAll(lines);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected abstract String eventSeparator();

    // Note: will not be called because of server error.
    @Override
    public void onError(Throwable throwable) {
        error = throwable;
        try {
            lineBuffer.put(POISON_TOKEN);
        } catch (InterruptedException e) {
            LOGGER.warn(e.getMessage(), e);
        }
        running = false;
        LOGGER.debug("On Error --> " + throwable.getMessage());
        LOGGER.warn(throwable.getMessage(), throwable);
    }

    @Override
    public void onComplete() {
        try {
            lineBuffer.put(POISON_TOKEN);
        } catch (InterruptedException e) {
            LOGGER.warn(e.getMessage(), e);
        }
        running = false;
        LOGGER.debug("On Complete --> ");
    }
}

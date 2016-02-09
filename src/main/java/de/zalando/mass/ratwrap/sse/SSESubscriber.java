package de.zalando.mass.ratwrap.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.primitives.Ints;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

public class SSESubscriber<T extends Serializable> implements Subscriber<ByteBuf>, EventReceiver<T> {
    protected static final int BULK_SIZE = 1;
    protected static final int QUEUE_SIZE = 100;
    protected static final String EVENT_TOKEN = "event:";
    protected static final String DATA_TOKEN = "data:";
    protected static final String ID_TOKEN = "id:";
    protected static final String RETRY_TOKEN = "retry:";
    protected static final String SEPARATOR_TOKEN = "EVENT_SEPARATOR";
    protected static final String POISON_TOKEN = "END_OF_STREAM";

    private final ObjectMapper objectMapper;
    private final Class<T> targetClass;
    private Subscription subscription;
    private final BlockingQueue<String> lineBuffer;
    private final BlockingQueue<HttpEvent<T>> eventBuffer;
    private boolean running;

    // ToDo: solve blocking thread where onNext called
    public HttpEvent<T> next() throws InterruptedException {
        return eventBuffer.take();
    }

    private synchronized boolean isRunning() {
        return running;
    }

    public SSESubscriber(ObjectMapper objectMapper, Class<T> targetClass) {
        this.objectMapper = objectMapper;
        this.targetClass = targetClass;
        lineBuffer = new ArrayBlockingQueue<>(QUEUE_SIZE);
        eventBuffer = new ArrayBlockingQueue<>(QUEUE_SIZE);
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

    private Boolean fetchQueue() {
        try {
            System.out.println("|+++> start fetching queue");
            String idToken = null;
            String eventToken = null;
            Integer retryToken = null;
            String dataToken = "";
            while (isRunning() || !lineBuffer.isEmpty()) {
                final String line = lineBuffer.take();
//                System.out.println("|==> " + line);
                if (Strings.isNullOrEmpty(line) || line.equals(SEPARATOR_TOKEN) || line.equals(POISON_TOKEN)) {
                    if (!Strings.isNullOrEmpty(dataToken)) {
                        try {
                            final HttpEvent<T> httpEvent = new HttpEvent<>(
                                    idToken,
                                    eventToken,
                                    retryToken,
                                    objectMapper.readValue(dataToken, targetClass));
//                            System.out.println("|==< " + httpEvent.toString());
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
                        eventBuffer.put(new HttpEvent<>(null, null, null, null));
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
            System.out.println("|+++< stop fetching queue");
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public void onNext(ByteBuf data) {
        final ByteBufInputStream bbis = new ByteBufInputStream(data);

        try {
            subscription.request(BULK_SIZE);
            final byte[] bytes = StreamUtils.copyToByteArray(bbis);
            final String event = new String(bytes).replace("\n\n","\nEVENT_SEPARATOR\n");
//            System.out.println("|--> " + event.replace("\n", " | "));
            final List<String> lines = Arrays.asList(event.split("\n"));
            lineBuffer.addAll(lines);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Note: will not be called because of server error.
    @Override
    public void onError(Throwable throwable) {
        try {
            lineBuffer.put(POISON_TOKEN);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        running = false;
        System.out.println("On Error --> " + throwable.getMessage());
        throwable.printStackTrace();
    }

    @Override
    public void onComplete() {
        try {
            lineBuffer.put(POISON_TOKEN);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        running = false;
        System.out.println("On Complete --> ");
    }

}

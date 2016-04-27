package de.zalando.mass.ratwrap;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.zalando.mass.ratwrap.data.InputData;
import de.zalando.mass.ratwrap.sse.ClosableBlockingQueue;
import org.slf4j.Logger;
import ratpack.stream.Streams;
import ratpack.stream.TransformablePublisher;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Utils {
    public static TransformablePublisher<InputData> testPublisher(int start, Logger LOGGER) {
        return Streams.yield(yieldRequest -> {
            if (yieldRequest.getRequestNum() > 9)
                return null;
//            TimeUnit.SECONDS.sleep(2);
            LOGGER.debug("server -> " + yieldRequest.toString());
            return new InputData("data", (int) yieldRequest.getRequestNum() + start + 1);
        });
    }

    public static ClosableBlockingQueue<InputData> testQueue(int start) {
        final ClosableBlockingQueue<InputData> dataQueue = new ClosableBlockingQueue<>(5);
        final ExecutorService executorService = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
                .setNameFormat("test-queue-supplier-%d")
                .setDaemon(true)
                .build());
        CompletableFuture.supplyAsync(() -> {
            for (int i = 0; i < 10; i++) {
                try {
//                    TimeUnit.SECONDS.sleep(2);
                    dataQueue.put(new InputData("data", i + start + 1));
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

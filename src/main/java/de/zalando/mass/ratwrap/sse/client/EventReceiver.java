package de.zalando.mass.ratwrap.sse.client;

public interface EventReceiver<T> {
    HttpEvent<T> next() throws InterruptedException;
}

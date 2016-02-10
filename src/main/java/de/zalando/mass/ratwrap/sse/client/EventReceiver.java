package de.zalando.mass.ratwrap.sse.client;

import java.io.Serializable;

public interface EventReceiver<T extends Serializable> {
    HttpEvent<T> next() throws InterruptedException;
}

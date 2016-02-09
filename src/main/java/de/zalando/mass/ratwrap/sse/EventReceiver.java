package de.zalando.mass.ratwrap.sse;

import java.io.Serializable;

public interface EventReceiver<T extends Serializable> {
    HttpEvent<T> next() throws InterruptedException;
}

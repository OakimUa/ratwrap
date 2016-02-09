package de.zalando.mass.ratwrap.sse;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;

public class ClosableBlockingQueue<E> extends ArrayBlockingQueue<E> implements Closeable {
    private boolean closed = false;

    public ClosableBlockingQueue(int capacity) {
        super(capacity);
    }

    public ClosableBlockingQueue(int capacity, boolean fair) {
        super(capacity, fair);
    }

    public ClosableBlockingQueue(int capacity, boolean fair, Collection<? extends E> c) {
        super(capacity, fair, c);
    }

    @Override
    synchronized public void close() throws IOException {
        closed = true;
    }

    synchronized public boolean isClosed() {
        return closed;
    }

    @Override
    public void put(E data)
            throws InterruptedException, IllegalStateException {
        if (isClosed()) {
            throw new IllegalStateException("this queue is closed");
        }
        super.put(data);
    }

    @Override
    public E take() throws InterruptedException {
        return super.take();
    }

    public Optional<E> maybeTake() throws InterruptedException {
        if (hasNext()) {
            return Optional.of(take());
        } else {
            return Optional.empty();
        }
    }

    synchronized public boolean hasNext() {
        return !(isClosed() && isEmpty());
    }
}

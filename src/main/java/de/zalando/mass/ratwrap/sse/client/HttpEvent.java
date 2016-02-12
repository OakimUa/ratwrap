package de.zalando.mass.ratwrap.sse.client;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString
@EqualsAndHashCode
public class HttpEvent<T> {
    private final String id;
    private final String event;
    private final Integer retry;
    private final T data;

    public boolean isPoison() {
        return false;
    }
}

package de.zalando.mass.ratwrap.sse;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

@Data
@ToString
public class HttpEvent<T extends Serializable> {
    private final String id;
    private final String event;
    private final Integer retry;
    private final T data;

    public boolean isPoison() {
        return id == null && event == null && retry == null && data == null;
    }
}

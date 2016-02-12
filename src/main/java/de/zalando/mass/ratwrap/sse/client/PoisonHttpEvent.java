package de.zalando.mass.ratwrap.sse.client;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString
@EqualsAndHashCode
public class PoisonHttpEvent<T> extends HttpEvent<T> {
    private final Throwable error;

    public PoisonHttpEvent(Throwable error) {
        super(null, null, null, null);
        this.error = error;
    }

    @Override
    public boolean isPoison() {
        return true;
    }
}

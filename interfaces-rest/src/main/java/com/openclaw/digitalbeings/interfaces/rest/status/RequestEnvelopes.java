package com.openclaw.digitalbeings.interfaces.rest.status;

import com.github.f4b6a3.ulid.UlidCreator;
import java.time.Instant;

public final class RequestEnvelopes {

    private RequestEnvelopes() {
    }

    public static <T> RequestEnvelope<T> success(T data) {
        return new RequestEnvelope<>(UlidCreator.getUlid().toString(), Instant.now(), true, data, null);
    }

    public static RequestEnvelope<Object> error(Object error) {
        return new RequestEnvelope<>(UlidCreator.getUlid().toString(), Instant.now(), false, null, error);
    }
}

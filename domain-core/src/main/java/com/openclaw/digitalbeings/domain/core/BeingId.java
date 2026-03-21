package com.openclaw.digitalbeings.domain.core;

import com.github.f4b6a3.ulid.UlidCreator;

public record BeingId(String value) {

    public BeingId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("BeingId value must not be blank.");
        }
    }

    public static BeingId newId() {
        return new BeingId(UlidCreator.getUlid().toString());
    }
}

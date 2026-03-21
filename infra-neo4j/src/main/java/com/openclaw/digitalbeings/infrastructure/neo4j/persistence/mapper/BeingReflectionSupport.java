package com.openclaw.digitalbeings.infrastructure.neo4j.persistence.mapper;

import com.openclaw.digitalbeings.domain.being.Being;
import com.openclaw.digitalbeings.domain.core.BeingId;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.Instant;

final class BeingReflectionSupport {

    private BeingReflectionSupport() {
    }

    static Being instantiateBeing(String beingId, String displayName, Instant createdAt) {
        try {
            Constructor<Being> constructor = Being.class.getDeclaredConstructor(
                    BeingId.class,
                    String.class,
                    Instant.class
            );
            constructor.setAccessible(true);
            return constructor.newInstance(new BeingId(beingId), displayName, createdAt);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to rehydrate Being aggregate.", exception);
        }
    }

    static void setField(Being being, String fieldName, Object value) {
        try {
            Field field = Being.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(being, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to set field " + fieldName + " on Being.", exception);
        }
    }
}

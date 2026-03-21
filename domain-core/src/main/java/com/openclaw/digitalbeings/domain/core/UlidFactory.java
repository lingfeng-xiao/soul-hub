package com.openclaw.digitalbeings.domain.core;

import com.github.f4b6a3.ulid.UlidCreator;

public final class UlidFactory {

    private UlidFactory() {
    }

    public static String newUlid() {
        return UlidCreator.getUlid().toString();
    }
}

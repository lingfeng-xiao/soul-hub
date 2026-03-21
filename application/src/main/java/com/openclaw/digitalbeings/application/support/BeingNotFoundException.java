package com.openclaw.digitalbeings.application.support;

public final class BeingNotFoundException extends RuntimeException {

    public BeingNotFoundException(String beingId) {
        super("Being not found: " + beingId);
    }
}

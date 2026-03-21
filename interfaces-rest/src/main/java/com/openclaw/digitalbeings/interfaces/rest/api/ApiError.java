package com.openclaw.digitalbeings.interfaces.rest.api;

public record ApiError(
        String code,
        String message
) {
}

package com.openclaw.digitalbeings.application.being;

public record CreateBeingCommand(
        String displayName,
        String actor
) {

    public CreateBeingCommand {
        displayName = requireText(displayName, "displayName");
        actor = requireText(actor, "actor");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return value.trim();
    }
}

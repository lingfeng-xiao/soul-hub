package com.openclaw.digitalbeings.domain.core;

public final class DomainRuleViolation extends RuntimeException {

    public DomainRuleViolation(String message) {
        super(message);
    }
}

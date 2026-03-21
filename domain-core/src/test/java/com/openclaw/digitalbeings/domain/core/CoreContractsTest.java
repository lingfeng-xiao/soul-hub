package com.openclaw.digitalbeings.domain.core;

import java.util.EnumSet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CoreContractsTest {

    @Test
    void beingIdRejectsBlankValues() {
        assertThrows(IllegalArgumentException.class, () -> new BeingId(" "));
    }

    @Test
    void reviewStatusesExposeExpectedTerminalStates() {
        EnumSet<ReviewItemStatus> terminalStates = EnumSet.of(
                ReviewItemStatus.ACCEPTED,
                ReviewItemStatus.REJECTED,
                ReviewItemStatus.DEFERRED,
                ReviewItemStatus.CANCELLED
        );
        assertEquals(4, terminalStates.size());
        assertFalse(terminalStates.contains(ReviewItemStatus.SUBMITTED));
    }
}

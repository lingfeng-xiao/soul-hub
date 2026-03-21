package com.openclaw.digitalbeings.application.snapshot;

import com.openclaw.digitalbeings.application.being.BeingService;
import com.openclaw.digitalbeings.application.being.BeingView;
import com.openclaw.digitalbeings.application.being.CreateBeingCommand;
import com.openclaw.digitalbeings.application.lease.AcquireAuthorityLeaseCommand;
import com.openclaw.digitalbeings.application.lease.LeaseService;
import com.openclaw.digitalbeings.application.lease.RegisterRuntimeSessionCommand;
import com.openclaw.digitalbeings.application.lease.RuntimeSessionView;
import com.openclaw.digitalbeings.application.support.InMemoryBeingStore;
import com.openclaw.digitalbeings.domain.core.DomainRuleViolation;
import com.openclaw.digitalbeings.domain.core.SnapshotType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SnapshotServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-03-21T08:40:00Z"), ZoneOffset.UTC);

    @Test
    void createSnapshotPersistsAndListsSnapshots() {
        InMemoryBeingStore store = new InMemoryBeingStore();
        BeingService beingService = new BeingService(store, CLOCK);
        SnapshotService snapshotService = new SnapshotService(store, CLOCK);
        BeingView view = beingService.createBeing(new CreateBeingCommand("guan-guan", "codex"));

        SnapshotView snapshot = snapshotService.createSnapshot(
                new CreateSnapshotCommand(view.beingId(), SnapshotType.MILESTONE, "phase-1 seam checkpoint", "codex")
        );

        assertEquals(view.beingId(), snapshot.beingId());
        assertEquals("MILESTONE", snapshot.type());
        assertEquals(1, snapshotService.listSnapshots(view.beingId()).size());
    }

    @Test
    void postRestoreSnapshotIsBlockedWhileAnActiveLeaseExists() {
        InMemoryBeingStore store = new InMemoryBeingStore();
        BeingService beingService = new BeingService(store, CLOCK);
        LeaseService leaseService = new LeaseService(store, CLOCK);
        SnapshotService snapshotService = new SnapshotService(store, CLOCK);
        BeingView view = beingService.createBeing(new CreateBeingCommand("guan-guan", "codex"));
        RuntimeSessionView session = leaseService.registerRuntimeSession(
                new RegisterRuntimeSessionCommand(view.beingId(), "codex", "codex")
        );
        leaseService.acquireAuthorityLease(
                new AcquireAuthorityLeaseCommand(view.beingId(), session.sessionId(), "codex")
        );

        assertThrows(
                DomainRuleViolation.class,
                () -> snapshotService.createSnapshot(
                        new CreateSnapshotCommand(view.beingId(), SnapshotType.POST_RESTORE, "blocked", "codex")
                )
        );
    }
}

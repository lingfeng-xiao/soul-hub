package com.openclaw.digitalbeings.jobs;

import com.openclaw.digitalbeings.application.lease.LeaseService;
import com.openclaw.digitalbeings.domain.being.Being;
import com.openclaw.digitalbeings.domain.runtime.RuntimeSession;
import com.openclaw.digitalbeings.application.support.BeingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Instant;

@Component
public class SessionCleanupJob {
    private static final Logger log = LoggerFactory.getLogger(SessionCleanupJob.class);
    private final LeaseService leaseService;
    private final BeingStore beingStore;

    public SessionCleanupJob(LeaseService leaseService, BeingStore beingStore) {
        this.leaseService = leaseService;
        this.beingStore = beingStore;
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupOrphanedSessions() {
        log.info("Running session cleanup job...");
        for (Being being : beingStore.findAll()) {
            for (RuntimeSession session : being.runtimeSessions()) {
                if (!session.isActive() && session.isOrphaned(Instant.now())) {
                    try {
                        leaseService.closeSession(being.beingId().value(), session.sessionId(), "SYSTEM");
                        log.info("Closed orphaned session {} for being {}", session.sessionId(), being.beingId());
                    } catch (Exception e) {
                        log.error("Failed to close orphaned session {} for being {}", session.sessionId(), being.beingId(), e);
                    }
                }
            }
        }
    }
}

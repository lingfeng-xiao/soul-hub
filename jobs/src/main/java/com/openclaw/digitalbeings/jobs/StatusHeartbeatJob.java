package com.openclaw.digitalbeings.jobs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "digital-beings.jobs", name = "heartbeat-enabled", havingValue = "true")
public class StatusHeartbeatJob {

    private static final Logger log = LoggerFactory.getLogger(StatusHeartbeatJob.class);

    @Scheduled(fixedDelayString = "${digital-beings.jobs.status-heartbeat-delay-ms:1800000}")
    void emitHeartbeat() {
        log.info("Digital Beings Java heartbeat job executed.");
    }
}

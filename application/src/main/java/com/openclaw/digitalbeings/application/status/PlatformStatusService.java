package com.openclaw.digitalbeings.application.status;

import org.springframework.stereotype.Service;

@Service
public class PlatformStatusService {

    public PlatformStatusSnapshot currentStatus() {
        return new PlatformStatusSnapshot(
                "stage-2",
                "Remote Neo4j verification is live and the next focus is persistence-backed end-to-end activation",
                "docs/PROGRAM-STATUS.md",
                "Run a persistence-backed smoke flow through the neo4j profile against the live remote Neo4j node."
        );
    }
}

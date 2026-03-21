package com.openclaw.digitalbeings.boot.config;

import com.openclaw.digitalbeings.application.being.BeingService;
import com.openclaw.digitalbeings.application.governance.GovernanceService;
import com.openclaw.digitalbeings.application.hostcontract.HostContractService;
import com.openclaw.digitalbeings.application.lease.LeaseService;
import com.openclaw.digitalbeings.application.relationship.RelationshipService;
import com.openclaw.digitalbeings.application.review.ReviewService;
import com.openclaw.digitalbeings.application.snapshot.SnapshotService;
import com.openclaw.digitalbeings.application.support.BeingStore;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class DigitalBeingsServiceConfiguration {

    @Bean
    Clock digitalBeingsClock() {
        return Clock.systemUTC();
    }

    @Bean
    BeingService beingService(BeingStore beingStore, Clock digitalBeingsClock) {
        return new BeingService(beingStore, digitalBeingsClock);
    }

    @Bean
    LeaseService leaseService(BeingStore beingStore, Clock digitalBeingsClock) {
        return new LeaseService(beingStore, digitalBeingsClock);
    }

    @Bean
    ReviewService reviewService(BeingStore beingStore, Clock digitalBeingsClock) {
        return new ReviewService(beingStore, digitalBeingsClock);
    }

    @Bean
    RelationshipService relationshipService(BeingStore beingStore, Clock digitalBeingsClock) {
        return new RelationshipService(beingStore, digitalBeingsClock);
    }

    @Bean
    HostContractService hostContractService(BeingStore beingStore, Clock digitalBeingsClock) {
        return new HostContractService(beingStore, digitalBeingsClock);
    }

    @Bean
    GovernanceService governanceService(BeingStore beingStore, Clock digitalBeingsClock) {
        return new GovernanceService(beingStore, digitalBeingsClock);
    }

    @Bean
    SnapshotService snapshotService(BeingStore beingStore, Clock digitalBeingsClock) {
        return new SnapshotService(beingStore, digitalBeingsClock);
    }
}

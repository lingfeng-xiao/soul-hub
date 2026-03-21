package com.openclaw.digitalbeings.interfaces.cli;

import com.openclaw.digitalbeings.application.being.BeingService;
import com.openclaw.digitalbeings.application.governance.GovernanceService;
import com.openclaw.digitalbeings.application.hostcontract.HostContractService;
import com.openclaw.digitalbeings.application.lease.LeaseService;
import com.openclaw.digitalbeings.application.relationship.RelationshipService;
import com.openclaw.digitalbeings.application.review.ReviewService;
import com.openclaw.digitalbeings.application.snapshot.SnapshotService;
import com.openclaw.digitalbeings.application.support.BeingStore;
import com.openclaw.digitalbeings.application.support.InMemoryBeingStore;
import java.time.Clock;
import java.util.Objects;

public final class CliRuntime {

    private final BeingService beingService;
    private final LeaseService leaseService;
    private final ReviewService reviewService;
    private final RelationshipService relationshipService;
    private final HostContractService hostContractService;
    private final GovernanceService governanceService;
    private final SnapshotService snapshotService;

    private CliRuntime(
            BeingService beingService,
            LeaseService leaseService,
            ReviewService reviewService,
            RelationshipService relationshipService,
            HostContractService hostContractService,
            GovernanceService governanceService,
            SnapshotService snapshotService
    ) {
        this.beingService = Objects.requireNonNull(beingService, "beingService");
        this.leaseService = Objects.requireNonNull(leaseService, "leaseService");
        this.reviewService = Objects.requireNonNull(reviewService, "reviewService");
        this.relationshipService = Objects.requireNonNull(relationshipService, "relationshipService");
        this.hostContractService = Objects.requireNonNull(hostContractService, "hostContractService");
        this.governanceService = Objects.requireNonNull(governanceService, "governanceService");
        this.snapshotService = Objects.requireNonNull(snapshotService, "snapshotService");
    }

    public static CliRuntime memory() {
        Clock clock = Clock.systemUTC();
        BeingStore beingStore = new InMemoryBeingStore();
        return new CliRuntime(
                new BeingService(beingStore, clock),
                new LeaseService(beingStore, clock),
                new ReviewService(beingStore, clock),
                new RelationshipService(beingStore, clock),
                new HostContractService(beingStore, clock),
                new GovernanceService(beingStore, clock),
                new SnapshotService(beingStore, clock)
        );
    }

    public BeingService beingService() {
        return beingService;
    }

    public LeaseService leaseService() {
        return leaseService;
    }

    public ReviewService reviewService() {
        return reviewService;
    }

    public RelationshipService relationshipService() {
        return relationshipService;
    }

    public HostContractService hostContractService() {
        return hostContractService;
    }

    public GovernanceService governanceService() {
        return governanceService;
    }

    public SnapshotService snapshotService() {
        return snapshotService;
    }
}

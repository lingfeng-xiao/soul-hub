package com.openclaw.digitalbeings.application.lease;

import com.openclaw.digitalbeings.application.being.BeingView;
import com.openclaw.digitalbeings.application.support.BeingStore;
import com.openclaw.digitalbeings.domain.being.Being;
import com.openclaw.digitalbeings.domain.runtime.AuthorityLease;
import com.openclaw.digitalbeings.domain.runtime.RuntimeSession;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class LeaseService {

    private final BeingStore beingStore;
    private final Clock clock;

    public LeaseService(BeingStore beingStore, Clock clock) {
        this.beingStore = Objects.requireNonNull(beingStore, "beingStore");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public RuntimeSessionView registerRuntimeSession(RegisterRuntimeSessionCommand command) {
        Objects.requireNonNull(command, "command");
        Being being = beingStore.requireById(command.beingId());
        RuntimeSession runtimeSession = being.registerRuntimeSession(command.hostType(), command.actor(), clock.instant());
        beingStore.save(being);
        return RuntimeSessionView.from(command.beingId(), runtimeSession);
    }

    public LeaseView acquireAuthorityLease(AcquireAuthorityLeaseCommand command) {
        Objects.requireNonNull(command, "command");
        Being being = beingStore.requireById(command.beingId());
        AuthorityLease lease = being.acquireAuthorityLease(command.sessionId(), command.actor(), clock.instant());
        beingStore.save(being);
        RuntimeSession session = being.runtimeSessions().stream()
                .filter(candidate -> candidate.sessionId().equals(command.sessionId()))
                .findFirst()
                .orElseThrow();
        return LeaseView.from(command.beingId(), lease, session);
    }

    public LeaseView releaseAuthorityLease(ReleaseAuthorityLeaseCommand command) {
        Objects.requireNonNull(command, "command");
        Being being = beingStore.requireById(command.beingId());
        being.releaseAuthorityLease(command.leaseId(), command.actor(), clock.instant());
        beingStore.save(being);
        AuthorityLease lease = being.authorityLeases().stream()
                .filter(candidate -> candidate.leaseId().equals(command.leaseId()))
                .findFirst()
                .orElseThrow();
        RuntimeSession session = being.runtimeSessions().stream()
                .filter(candidate -> candidate.sessionId().equals(lease.sessionId()))
                .findFirst()
                .orElseThrow();
        return LeaseView.from(command.beingId(), lease, session);
    }

    public BeingView getBeing(String beingId) {
        return BeingView.from(beingStore.requireById(beingId));
    }

    public RuntimeSessionView closeSession(String beingId, String sessionId, String actor) {
        Being being = beingStore.requireById(beingId);
        being.closeRuntimeSession(sessionId, actor, clock.instant());
        beingStore.save(being);
        RuntimeSession session = being.requireRuntimeSession(sessionId);
        return RuntimeSessionView.from(beingId, session);
    }

    public LeaseView expireLease(String beingId, String leaseId, String actor) {
        Being being = beingStore.requireById(beingId);
        AuthorityLease lease = being.requireAuthorityLease(leaseId);
        lease.expire(actor, clock.instant());
        beingStore.save(being);
        RuntimeSession session = being.requireRuntimeSession(lease.sessionId());
        return LeaseView.from(beingId, lease, session);
    }

    public LeaseView revokeLease(String beingId, String leaseId, String actor) {
        Being being = beingStore.requireById(beingId);
        AuthorityLease lease = being.requireAuthorityLease(leaseId);
        lease.revoke(actor, clock.instant());
        beingStore.save(being);
        RuntimeSession session = being.requireRuntimeSession(lease.sessionId());
        return LeaseView.from(beingId, lease, session);
    }

    public RuntimeSessionView getSession(String beingId, String sessionId) {
        Being being = beingStore.requireById(beingId);
        RuntimeSession session = being.requireRuntimeSession(sessionId);
        return RuntimeSessionView.from(beingId, session);
    }

    public List<RuntimeSessionView> listSessions(String beingId) {
        Being being = beingStore.requireById(beingId);
        return being.runtimeSessions().stream()
                .map(session -> RuntimeSessionView.from(beingId, session))
                .collect(Collectors.toList());
    }

    public List<RuntimeSessionView> listActiveSessions(String beingId) {
        Being being = beingStore.requireById(beingId);
        return being.runtimeSessions().stream()
                .filter(RuntimeSession::isActive)
                .map(session -> RuntimeSessionView.from(beingId, session))
                .collect(Collectors.toList());
    }
}

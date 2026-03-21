package com.openclaw.digitalbeings.infrastructure.neo4j.persistence.mapper;

import com.openclaw.digitalbeings.domain.runtime.AuthorityLease;
import com.openclaw.digitalbeings.domain.runtime.HostContract;
import com.openclaw.digitalbeings.domain.runtime.RuntimeSession;
import com.openclaw.digitalbeings.infrastructure.neo4j.persistence.entity.AuthorityLeaseNode;
import com.openclaw.digitalbeings.infrastructure.neo4j.persistence.entity.HostContractNode;
import com.openclaw.digitalbeings.infrastructure.neo4j.persistence.entity.RuntimeSessionNode;
import java.lang.reflect.Constructor;
import java.time.Instant;
import java.util.List;

final class RuntimeSliceMapper {

    private RuntimeSliceMapper() {
    }

    static List<HostContractNode> toHostContractNodes(List<HostContract> hostContracts) {
        return hostContracts.stream()
                .map(contract -> new HostContractNode(
                        contract.contractId(),
                        contract.hostType(),
                        contract.status(),
                        contract.registeredAt()
                ))
                .toList();
    }

    static List<HostContract> toHostContracts(List<HostContractNode> nodes) {
        return nodes.stream()
                .map(node -> new HostContract(
                        node.getContractId(),
                        node.getHostType(),
                        node.getStatus(),
                        node.getRegisteredAt()
                ))
                .toList();
    }

    static List<RuntimeSessionNode> toSessionNodes(List<RuntimeSession> sessions) {
        return sessions.stream()
                .map(session -> new RuntimeSessionNode(
                        session.sessionId(),
                        session.hostType(),
                        session.startedAt(),
                        session.endedAt()
                ))
                .toList();
    }

    static List<RuntimeSession> toRuntimeSessions(List<RuntimeSessionNode> nodes) {
        return nodes.stream().map(RuntimeSliceMapper::toRuntimeSession).toList();
    }

    static List<AuthorityLeaseNode> toLeaseNodes(List<AuthorityLease> leases) {
        return leases.stream()
                .map(lease -> new AuthorityLeaseNode(
                        lease.leaseId(),
                        lease.sessionId(),
                        lease.status(),
                        lease.requestedAt(),
                        lease.grantedAt(),
                        lease.releasedAt(),
                        lease.lastActor()
                ))
                .toList();
    }

    static List<AuthorityLease> toAuthorityLeases(List<AuthorityLeaseNode> nodes) {
        return nodes.stream().map(RuntimeSliceMapper::toAuthorityLease).toList();
    }

    private static RuntimeSession toRuntimeSession(RuntimeSessionNode node) {
        try {
            Constructor<RuntimeSession> constructor = RuntimeSession.class.getDeclaredConstructor(
                    String.class,
                    String.class,
                    Instant.class,
                    Instant.class
            );
            constructor.setAccessible(true);
            return constructor.newInstance(
                    node.getSessionId(),
                    node.getHostType(),
                    node.getStartedAt(),
                    node.getEndedAt()
            );
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to rehydrate RuntimeSession.", exception);
        }
    }

    private static AuthorityLease toAuthorityLease(AuthorityLeaseNode node) {
        try {
            Constructor<AuthorityLease> constructor = AuthorityLease.class.getDeclaredConstructor(
                    String.class,
                    String.class,
                    com.openclaw.digitalbeings.domain.core.AuthorityLeaseStatus.class,
                    Instant.class,
                    Instant.class,
                    Instant.class,
                    String.class
            );
            constructor.setAccessible(true);
            return constructor.newInstance(
                    node.getLeaseId(),
                    node.getSessionId(),
                    node.getStatus(),
                    node.getRequestedAt(),
                    node.getGrantedAt(),
                    node.getReleasedAt(),
                    node.getLastActor()
            );
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to rehydrate AuthorityLease.", exception);
        }
    }
}

package com.openclaw.digitalbeings.infrastructure.neo4j.persistence.mapper;

import com.openclaw.digitalbeings.domain.snapshot.ContinuitySnapshot;
import com.openclaw.digitalbeings.infrastructure.neo4j.persistence.entity.ContinuitySnapshotNode;
import java.util.List;

final class SnapshotSliceMapper {

    private SnapshotSliceMapper() {
    }

    static List<ContinuitySnapshotNode> toSnapshotNodes(List<ContinuitySnapshot> snapshots) {
        return snapshots.stream()
                .map(snapshot -> new ContinuitySnapshotNode(
                        snapshot.snapshotId(),
                        snapshot.type(),
                        snapshot.summary(),
                        snapshot.createdAt()
                ))
                .toList();
    }

    static List<ContinuitySnapshot> toSnapshots(List<ContinuitySnapshotNode> nodes) {
        return nodes.stream()
                .map(node -> new ContinuitySnapshot(
                        node.getSnapshotId(),
                        node.getType(),
                        node.getSummary(),
                        node.getCreatedAt()
                ))
                .toList();
    }
}

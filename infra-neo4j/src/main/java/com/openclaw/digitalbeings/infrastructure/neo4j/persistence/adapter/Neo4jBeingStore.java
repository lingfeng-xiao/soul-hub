package com.openclaw.digitalbeings.infrastructure.neo4j.persistence.adapter;

import com.openclaw.digitalbeings.application.support.BeingStore;
import com.openclaw.digitalbeings.domain.being.Being;
import com.openclaw.digitalbeings.infrastructure.neo4j.persistence.mapper.BeingNodeMapper;
import com.openclaw.digitalbeings.infrastructure.neo4j.persistence.repository.BeingNodeRepository;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class Neo4jBeingStore implements BeingStore {

    private final BeingNodeRepository repository;

    public Neo4jBeingStore(BeingNodeRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    @Override
    public Being save(Being being) {
        Objects.requireNonNull(being, "being");
        return BeingNodeMapper.toDomain(repository.save(BeingNodeMapper.fromDomain(being)));
    }

    @Override
    public Optional<Being> findById(String beingId) {
        return repository.findByBeingId(requireText(beingId, "beingId")).map(BeingNodeMapper::toDomain);
    }

    @Override
    public List<Being> findAll() {
        return repository.findAll().stream().map(BeingNodeMapper::toDomain).toList();
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
        return value.trim();
    }
}

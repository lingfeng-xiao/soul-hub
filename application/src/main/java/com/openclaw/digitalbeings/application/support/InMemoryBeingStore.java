package com.openclaw.digitalbeings.application.support;

import com.openclaw.digitalbeings.domain.being.Being;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryBeingStore implements BeingStore {

    private final Map<String, Being> beings = new ConcurrentHashMap<>();

    @Override
    public Being save(Being being) {
        beings.put(being.beingId().value(), being);
        return being;
    }

    @Override
    public Optional<Being> findById(String beingId) {
        return Optional.ofNullable(beings.get(beingId));
    }

    @Override
    public List<Being> findAll() {
        return beings.values().stream()
                .sorted(Comparator.comparing(being -> being.beingId().value()))
                .toList();
    }
}

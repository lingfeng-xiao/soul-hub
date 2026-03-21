package com.openclaw.digitalbeings.application.support;

import com.openclaw.digitalbeings.domain.being.Being;
import java.util.List;
import java.util.Optional;

public interface BeingStore {

    Being save(Being being);

    Optional<Being> findById(String beingId);

    List<Being> findAll();

    default Being requireById(String beingId) {
        return findById(beingId).orElseThrow(() -> new BeingNotFoundException(beingId));
    }
}

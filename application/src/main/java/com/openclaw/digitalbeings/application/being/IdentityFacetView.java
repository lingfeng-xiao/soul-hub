package com.openclaw.digitalbeings.application.being;

import com.openclaw.digitalbeings.domain.identity.IdentityFacet;
import java.time.Instant;

public record IdentityFacetView(
        String facetId,
        String kind,
        String summary,
        Instant recordedAt
) {

    public static IdentityFacetView from(IdentityFacet facet) {
        return new IdentityFacetView(
                facet.facetId(),
                facet.kind(),
                facet.summary(),
                facet.recordedAt()
        );
    }
}

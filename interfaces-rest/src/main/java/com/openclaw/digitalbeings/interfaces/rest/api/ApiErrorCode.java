package com.openclaw.digitalbeings.interfaces.rest.api;

public enum ApiErrorCode {
    IDENTITY_VALIDATION,
    LEASE_VALIDATION,
    REVIEW_VALIDATION,
    SNAPSHOT_VALIDATION,
    GRAPH_VALIDATION,
    IMPORT_VALIDATION;

    public static ApiErrorCode forRequestPath(String requestPath) {
        if (requestPath == null || requestPath.isBlank()) {
            return GRAPH_VALIDATION;
        }
        if (requestPath.startsWith("/beings")) {
            return IDENTITY_VALIDATION;
        }
        if (requestPath.startsWith("/sessions") || requestPath.startsWith("/leases")) {
            return LEASE_VALIDATION;
        }
        if (requestPath.startsWith("/reviews") || requestPath.startsWith("/canonical-projections")) {
            return REVIEW_VALIDATION;
        }
        if (requestPath.startsWith("/snapshots")) {
            return SNAPSHOT_VALIDATION;
        }
        if (requestPath.startsWith("/relationships")
                || requestPath.startsWith("/host-contracts")
                || requestPath.startsWith("/owner-profile-facts")
                || requestPath.startsWith("/managed-agent-specs")) {
            return GRAPH_VALIDATION;
        }
        if (requestPath.startsWith("/imports")) {
            return IMPORT_VALIDATION;
        }
        return GRAPH_VALIDATION;
    }
}

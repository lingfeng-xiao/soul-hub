package com.openclaw.digitalbeings.testkit;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

public final class RemoteNeo4jAvailabilityCondition implements ExecutionCondition {

    private static final ConditionEvaluationResult ENABLED =
            ConditionEvaluationResult.enabled("Remote Neo4j verification node is configured.");

    private static final ConditionEvaluationResult DISABLED =
            ConditionEvaluationResult.disabled("Remote Neo4j verification node cache is not available or credentials cannot authenticate.");

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        return RemoteNeo4jConnectionSupport.isConfigured() && RemoteNeo4jConnectionSupport.canAuthenticate()
                ? ENABLED
                : DISABLED;
    }
}

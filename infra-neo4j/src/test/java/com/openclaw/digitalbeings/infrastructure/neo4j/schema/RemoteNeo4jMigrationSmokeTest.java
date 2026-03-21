package com.openclaw.digitalbeings.infrastructure.neo4j.schema;

import com.openclaw.digitalbeings.testkit.RemoteNeo4jConnectionSupport;
import com.openclaw.digitalbeings.testkit.RequiresRemoteNeo4j;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;

import static org.junit.jupiter.api.Assertions.assertTrue;

@RequiresRemoteNeo4j
class RemoteNeo4jMigrationSmokeTest {

    @Test
    void baselineGraphStatementsCanBeAppliedToTheRemoteVerificationNode() {
        try (Driver driver = RemoteNeo4jConnectionSupport.openDriver()) {
            applyStatements(driver, GraphConstraintCatalog.CONSTRAINTS);
            applyStatements(driver, GraphConstraintCatalog.INDEXES);

            try (var session = driver.session()) {
                List<String> constraintNames = session.run("SHOW CONSTRAINTS YIELD name RETURN name")
                        .list(record -> record.get("name").asString());
                List<String> indexNames = session.run("SHOW INDEXES YIELD name RETURN name")
                        .list(record -> record.get("name").asString());

                assertTrue(constraintNames.containsAll(GraphConstraintCatalog.constraintNames()));
                assertTrue(indexNames.containsAll(GraphConstraintCatalog.indexNames()));
            }
        }
    }

    private static void applyStatements(Driver driver, List<String> statements) {
        try (var session = driver.session()) {
            for (String statement : statements) {
                Result result = session.run(statement);
                result.consume();
            }
        }
    }
}

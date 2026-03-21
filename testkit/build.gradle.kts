description = "Reusable testing support, especially for Neo4j and Testcontainers."

dependencies {
    api(libs.neo4j.java.driver)
    api(libs.testcontainers.junit.jupiter)
    api(libs.testcontainers.neo4j)
    api(libs.junit.jupiter.api)
}

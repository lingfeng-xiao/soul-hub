description = "Neo4j persistence adapters, schema metadata, and migration baseline."

dependencies {
    api(project(":application"))
    testImplementation(project(":testkit"))
    implementation(libs.spring.boot.starter.data.neo4j)
    implementation(libs.neo4j.java.driver)
    implementation(libs.neo4j.migrations.core)
}

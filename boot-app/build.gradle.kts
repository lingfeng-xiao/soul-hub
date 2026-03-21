import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    alias(libs.plugins.spring.boot)
}

description = "Spring Boot entrypoint for the Digital Beings Java platform."

dependencies {
    implementation(project(":application"))
    implementation(project(":infra-neo4j"))
    implementation(project(":interfaces-rest"))
    implementation(project(":interfaces-cli"))
    implementation(project(":jobs"))
    implementation(project(":legacy-importer"))
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.data.neo4j)
    implementation(libs.neo4j.migrations.core)
    annotationProcessor(libs.spring.boot.configuration.processor)
}

tasks.named<BootJar>("bootJar") {
    archiveFileName.set("digital-beings-java.jar")
}

tasks.named<Jar>("jar") {
    enabled = false
}

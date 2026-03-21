plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "digital-beings-java"

include(
    "boot-app",
    "domain-core",
    "application",
    "infra-neo4j",
    "interfaces-rest",
    "interfaces-cli",
    "jobs",
    "legacy-importer",
    "testkit",
)

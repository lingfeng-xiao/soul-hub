import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.wrapper.Wrapper
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    alias(libs.plugins.spring.boot) apply false
}

group = "com.openclaw.digitalbeings"
version = "0.1.0-SNAPSHOT"

subprojects {
    apply(plugin = "java-library")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
        withSourcesJar()
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    dependencies {
        add("testImplementation", "org.springframework.boot:spring-boot-starter-test:3.3.5")
        add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher:1.11.3")
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        testLogging {
            events("failed", "passed", "skipped")
            exceptionFormat = TestExceptionFormat.FULL
            showExceptions = true
            showCauses = true
            showStackTraces = true
        }
    }
}

tasks.register("lint") {
    group = "verification"
    description = "Run the default verification checks for all modules."
    dependsOn(subprojects.map { "${it.path}:check" })
}

tasks.register("stage0Verify") {
    group = "verification"
    description = "Run the stage 0 baseline build verification."
    dependsOn("build")
}

tasks.named<Wrapper>("wrapper") {
    gradleVersion = "8.10.2"
    distributionType = Wrapper.DistributionType.BIN
}

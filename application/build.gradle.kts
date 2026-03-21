description = "Application services and use case orchestration."

dependencies {
    api(project(":domain-core"))
    implementation(libs.spring.boot.starter)
}

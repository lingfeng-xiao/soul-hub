description = "REST controllers and transport-level DTOs."

dependencies {
    api(project(":application"))
    implementation(libs.spring.boot.starter.web)
    implementation(libs.jackson.datatype.jsr310)
}

description = "CLI integration points and command catalog."

dependencies {
    api(project(":application"))
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.picocli)
    annotationProcessor(libs.picocli.codegen)
}

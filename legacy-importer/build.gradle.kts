description = "Legacy Python repository import contracts and mapping placeholders."

dependencies {
    api(project(":application"))
    implementation(libs.spring.boot.starter.json)
    implementation(libs.jackson.dataformat.yaml)
}

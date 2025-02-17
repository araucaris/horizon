dependencies {
    compileOnly(project(":aegis-codec-common"))
    compileOnly(libs.jetbrains.annotations)
    api(libs.jackson.databind)
}
plugins {
    `aegis-java`
    `aegis-publish`
    `aegis-repositories`
}

dependencies {
    compileOnly(project(":aegis-codec-common"))
    compileOnly(libs.jetbrains.annotations)
    api(libs.jackson.databind)
}

aegisPublish {
    artifactId = "aegis-codec-jackson"
}
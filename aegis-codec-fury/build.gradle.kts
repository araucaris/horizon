plugins {
    `aegis-java`
    `aegis-publish`
    `aegis-repositories`
}

dependencies {
    compileOnly(project(":aegis-codec-common"))
    compileOnly(libs.jetbrains.annotations)
    api(libs.fury.core)
}

aegisPublish {
    artifactId = "aegis-codec-fury"
}
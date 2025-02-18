plugins {
    `aegis-java`
    `aegis-publish`
    `aegis-repositories`
}

dependencies {
    api(project(":aegis-codec-common"))
    api(libs.fury.core)
}

aegisPublish {
    artifactId = "aegis-codec-fury"
}
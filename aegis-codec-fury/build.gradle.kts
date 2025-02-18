plugins {
    `aegis-java`
    `aegis-publish`
    `aegis-repositories`
}

dependencies {
    implementation(project(":aegis-codec-common"))
    implementation(libs.fury.core)
}

aegisPublish {
    artifactId = "aegis-codec-fury"
}
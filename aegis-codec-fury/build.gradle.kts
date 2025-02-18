plugins {
    `aegis-java`
    `aegis-publish`
    `aegis-repositories`
}

dependencies {
    compileOnly(project(":aegis-codec-common"))
    implementation(libs.fury.core)
}

aegisPublish {
    artifactId = "aegis-codec-fury"
}
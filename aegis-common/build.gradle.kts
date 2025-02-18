plugins {
    `aegis-java`
    `aegis-publish`
    `aegis-repositories`
}

dependencies {
    implementation(project(":aegis-codec-common"))
    implementation(project(":aegis-store"))
    implementation(project(":aegis-lock"))
    implementation(project(":aegis-broker"))
    implementation(project(":aegis-eventbus"))

    implementation(libs.lettuce.core)
    implementation(libs.vavr)
    implementation(libs.spotify.futures)
}

aegisPublish {
    artifactId = "aegis"
}
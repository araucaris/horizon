plugins {
    `aegis-java`
    `aegis-publish`
    `aegis-repositories`
}

dependencies {
    api(project(":aegis-codec-common"))
    api(project(":aegis-store"))
    api(project(":aegis-lock"))
    api(project(":aegis-broker"))
    api(project(":aegis-eventbus"))

    api(libs.lettuce.core)
    api(libs.vavr)
    api(libs.spotify.futures)
}

aegisPublish {
    artifactId = "aegis"
}
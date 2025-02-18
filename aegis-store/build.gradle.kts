plugins {
    `aegis-java`
    `aegis-publish`
    `aegis-repositories`
}

dependencies {
    compileOnly(project(":aegis-eventbus"))

    compileOnly(libs.lettuce.core)
    compileOnly(libs.spotify.futures)
}

aegisPublish {
    artifactId = "aegis-kv"
}
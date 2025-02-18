plugins {
    `aegis-java`
    `aegis-publish`
    `aegis-repositories`
}

dependencies {
    api(project(":aegis-store"))
    api(libs.spotify.futures)
    compileOnly(libs.lettuce.core)
}

aegisPublish {
    artifactId = "aegis-lock"
}
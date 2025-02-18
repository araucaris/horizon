plugins {
    `aegis-java`
    `aegis-publish`
    `aegis-repositories`
}

dependencies {
    implementation(project(":aegis-store"))
    implementation(libs.spotify.futures)
    compileOnly(libs.lettuce.core)
}

aegisPublish {
    artifactId = "aegis-lock"
}
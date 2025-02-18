plugins {
    `aegis-java`
    `aegis-publish`
    `aegis-repositories`
}

dependencies {
    api(project(":aegis-codec-common"))
    api(libs.jackson.databind)
}

aegisPublish {
    artifactId = "aegis-codec-jackson"
}
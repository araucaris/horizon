plugins {
    `aegis-java`
    `aegis-publish`
    `aegis-repositories`
}

dependencies {
    compileOnly(project(":aegis-codec-common"))
    implementation(libs.jackson.databind)
}

aegisPublish {
    artifactId = "aegis-codec-jackson"
}
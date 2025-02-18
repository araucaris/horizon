plugins {
    `aegis-java`
    `aegis-publish`
    `aegis-repositories`
}

dependencies {
    implementation(project(":aegis-codec-common"))
    implementation(project(":aegis-eventbus"))
    compileOnly(libs.lettuce.core)
}

aegisPublish {
    artifactId = "aegis-broker"
}
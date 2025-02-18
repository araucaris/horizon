plugins {
    `aegis-java`
    `aegis-publish`
    `aegis-repositories`
}

dependencies {
    api(project(":aegis-codec-common"))
    api(project(":aegis-eventbus"))
    compileOnly(libs.lettuce.core)
}

aegisPublish {
    artifactId = "aegis-broker"
}
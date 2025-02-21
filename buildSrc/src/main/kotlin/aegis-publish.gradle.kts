plugins {
    `java-library`
    `maven-publish`
}

group = "io.mikeamiry.aegis"
version = "2.0.12"

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    repositories {
        mavenLocal()
        maven(
            name = "rubymc-repository",
            url = "https://repo.rubymc.pl",
            username = "MAVEN_USERNAME",
            password = "MAVEN_PASSWORD"
        )
    }
}

fun RepositoryHandler.maven(
    name: String, url: String, username: String, password: String, snapshots: Boolean = true
) {
    val isSnapshot = version.toString().endsWith("-SNAPSHOT")
    if (isSnapshot && !snapshots) {
        return
    }

    this.maven {
        this.name = if (isSnapshot) "${name}Snapshots" else "${name}Releases"
        this.url = if (isSnapshot) uri("$url/snapshots") else uri("$url/releases")
        this.credentials {
            this.username = System.getenv(username)
            this.password = System.getenv(password)
        }
    }
}

interface AegisPublishExtension {
    var artifactId: String
}

val extension = extensions.create<AegisPublishExtension>("aegisPublish")

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("maven") {
                artifactId = extension.artifactId
                from(project.components["java"])
            }
        }
    }
}
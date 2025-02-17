plugins {
    `java-library`
    `maven-publish`
}

sourceSets {
    main {
        java.setSrcDirs(listOf("src"))
        resources.setSrcDirs(emptyList<String>()) // Add resources directory if needed
    }
    test {
        java.setSrcDirs(emptyList<String>())
        resources.setSrcDirs(emptyList<String>())
    }
}

subprojects {
    apply(plugin = "java-library")

    group = "io.mikeamiry.aegis"
    version = "2.0.2"

    repositories {
        mavenCentral()
    }

    java {
        withSourcesJar()
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }

    apply(plugin = "maven-publish")
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

    publishing {
        publications {
            create<MavenPublication>("maven") {
                artifactId = project.name.replace("-common", "")
                from(project.components["java"])
            }
        }
    }
}

fun RepositoryHandler.maven(
    name: String, url: String, username: String, password: String, snapshotsEnabled: Boolean = true
) {
    val isSnapshot = version.toString().endsWith("-SNAPSHOT")
    if (isSnapshot && !snapshotsEnabled) return

    maven {
        this.name = if (isSnapshot) "${name}Snapshots" else "${name}Releases"
        this.url = if (isSnapshot) uri("$url/snapshots") else uri("$url/releases")
        this.credentials {
            this.username = System.getenv(username)
                ?: throw IllegalStateException("Missing $username environment variable")
            this.password = System.getenv(password)
                ?: throw IllegalStateException("Missing $password environment variable")
        }
    }
}
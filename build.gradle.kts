plugins {
    `java-library`
}

sourceSets {
    main {
        java.setSrcDirs(emptyList<String>())
        resources.setSrcDirs(emptyList<String>())
    }
    test {
        java.setSrcDirs(emptyList<String>())
        resources.setSrcDirs(emptyList<String>())
    }
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    group = "io.mikeamiry.aegis"
    version = "2.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
//        maven("https://repo.shiza.dev/releases") (Doesn't respond at the moment of writing
    //        this, so the eventbus was moved into a separate module (aegis-eventbus)
    }

    java {
        withSourcesJar()
        toolchain {
            languageVersion = JavaLanguageVersion.of(17)
        }
    }

    sourceSets {
        main {
            java.setSrcDirs(listOf("src"))
            resources.setSrcDirs(emptyList<String>())
        }
        test {
            java.setSrcDirs(emptyList<String>())
            resources.setSrcDirs(emptyList<String>())
        }
    }
}
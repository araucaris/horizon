import org.gradle.accessors.dm.LibrariesForLibs

val libs = the<LibrariesForLibs>() // https://github.com/gradle/gradle/issues/15383

plugins {
    `java-library`
}

dependencies {
    compileOnly(libs.jetbrains.annotations)
}

java {
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
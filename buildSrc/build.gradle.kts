plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    api(kotlin("script-runtime"))
    api(files(libs.javaClass.superclass.protectionDomain.codeSource.location)) // https://github.com/gradle/gradle/issues/15383
}

sourceSets {
    main {
        java.setSrcDirs(emptyList<String>())
        groovy.setSrcDirs(emptyList<String>())
        resources.setSrcDirs(emptyList<String>())
    }
    test {
        java.setSrcDirs(emptyList<String>())
        kotlin.setSrcDirs(emptyList<String>())
        groovy.setSrcDirs(emptyList<String>())
        resources.setSrcDirs(emptyList<String>())
    }
}
dependencies {
    compileOnly(libs.jetbrains.annotations)
    api(project(":aegis-codec-common"))
    api(project(":aegis-eventbus"))
    api(libs.lettuce.core)
    api(libs.caffeine)
    api(libs.vavr)
    api(libs.spotify.futures)
}
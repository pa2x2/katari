plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.spotless)
}

android {
    namespace = "eu.kanade.tachiyomi.source.compat"
}

dependencies {
    api(projects.entrySourceApi)
    api(projects.sourceApi)
    implementation(projects.aniyomiSourceApi)

    testImplementation(libs.bundles.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver3)
    testRuntimeOnly(libs.junit.platform.launcher)
}

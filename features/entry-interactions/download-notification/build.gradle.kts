plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.compose)
    alias(mihonx.plugins.spotless)
}

android {
    namespace = "app.katari.entry.interactions.download.notification"
}

dependencies {
    api(projects.features.entryInteractions.api)

    implementation(projects.core.common)
    implementation(projects.core.i18n)
    implementation(libs.androidx.core)

    testImplementation(libs.bundles.test)
    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
    testImplementation(kotlin("test"))
    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.junit.vintage)
}

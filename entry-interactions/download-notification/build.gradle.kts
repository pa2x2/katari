plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.compose)
    alias(mihonx.plugins.spotless)
}

android {
    namespace = "mihon.entry.interactions.download.notification"
}

dependencies {
    api(projects.entryInteractions.api)

    implementation(projects.core.common)
    implementation(projects.i18n)
    implementation(libs.androidx.core)
    implementation(libs.injekt)

    testImplementation(libs.bundles.test)
    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
    testImplementation(kotlin("test"))
    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.junit.vintage)
}

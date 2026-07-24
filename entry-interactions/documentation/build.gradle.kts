plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.spotless)
}

android {
    namespace = "mihon.entry.interactions.documentation"
}

dependencies {
    implementation(projects.entryInteractions)
    implementation(projects.featureGraph)
    implementation(projects.featureValidation)
    implementation(platform(libs.androidx.compose.bom))

    testImplementation(libs.bundles.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}

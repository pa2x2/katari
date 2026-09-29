plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.compose)
    alias(mihonx.plugins.spotless)
}

android {
    namespace = "mihon.model.artifacts.ui"
}

dependencies {
    api(projects.modelArtifacts.api)

    implementation(projects.presentationCore)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.uiToolingPreview)

    debugImplementation(libs.androidx.compose.uiTooling)
}

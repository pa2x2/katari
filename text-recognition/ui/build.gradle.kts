plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.compose)
    alias(mihonx.plugins.spotless)
}

android {
    namespace = "mihon.text.recognition.ui"
}

dependencies {
    api(projects.textRecognition.api)
    api(projects.modelArtifacts.api)

    implementation(projects.modelArtifacts.ui)
    implementation(projects.presentationCore)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.materialIcons)
    implementation(libs.androidx.compose.uiToolingPreview)
    implementation(libs.kotlinx.coroutines.core)

    debugImplementation(libs.androidx.compose.uiTooling)
}

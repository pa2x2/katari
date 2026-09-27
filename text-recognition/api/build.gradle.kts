plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.spotless)
}

android {
    namespace = "mihon.text.recognition.api"
}

dependencies {
    api(projects.core.common)
    api(projects.language.api)
    api(projects.modelArtifacts.api)
    api(libs.kotlinx.coroutines.core)

    testImplementation(libs.bundles.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}

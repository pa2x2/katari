plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.spotless)
}

android {
    namespace = "mihon.translation.runtime"

    testFixtures {
        enable = true
    }
}

dependencies {
    api(projects.translation.api)

    implementation(projects.language.runtime)
    implementation(projects.featureGraph)
    implementation(projects.featureRuntime)
    implementation(projects.core.common)
    implementation(projects.translation.spi)
    implementation(libs.androidx.appCompat)
    implementation(libs.injekt)

    testImplementation(projects.featureValidation)
    testImplementation(libs.bundles.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly(libs.junit.platform.launcher)

    testFixturesImplementation(projects.featureValidation)
    testFixturesImplementation(projects.featureGraph)
    testFixturesImplementation(projects.translation.spi)
    testFixturesImplementation(libs.bundles.test)
}

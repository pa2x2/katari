plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.compose)
    alias(mihonx.plugins.spotless)
}

android {
    namespace = "mihon.entry.interactions.spi"
}

dependencies {
    api(projects.entryInteractions.api)
    api(projects.featureGraph)
    api(projects.featureRuntime)
    api(libs.androidx.appCompat)
    api(libs.coil.core)

    implementation(projects.presentationCore)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.materialIcons)
    implementation(libs.androidx.compose.material3)
    implementation(libs.injekt)

    testImplementation(libs.bundles.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(kotlin("test"))
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test>().configureEach {
    // MockK/Byte Buddy instruments the test JVM, which is incompatible with class-data sharing.
    jvmArgs("-XX:+EnableDynamicAgentLoading", "-Xshare:off")
}

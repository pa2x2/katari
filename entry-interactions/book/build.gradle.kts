plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.compose)
    alias(mihonx.plugins.spotless)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "mihon.entry.interactions.book"
}

dependencies {
    api(projects.bookApi)
    api(projects.domain)
    api(projects.entryInteractions.spi)
    api(projects.entryViewerSettingsApi)
    implementation(projects.translation.api)
    implementation(projects.tts.api)
    implementation(projects.tts.ui)

    implementation(projects.core.common)
    implementation(projects.i18n)
    implementation(projects.presentationCore)
    implementation(projects.translation.ui)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.materialIcons)
    implementation(libs.androidx.fragment)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.bundles.serialization)
    implementation(libs.injekt)
    implementation(libs.jsoup)
    implementation(libs.unifile)

    testImplementation(libs.bundles.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(kotlin("test"))
    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.junit.vintage)
}

tasks.withType<Test>().configureEach {
    // MockK/Byte Buddy instruments the test JVM, which is incompatible with class-data sharing.
    jvmArgs("-XX:+EnableDynamicAgentLoading", "-Xshare:off")
}

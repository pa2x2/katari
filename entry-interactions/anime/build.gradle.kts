plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.compose)
    alias(mihonx.plugins.spotless)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.metro)
}

android {
    namespace = "mihon.entry.interactions.anime"
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.FlowPreview",
        )
    }
}

dependencies {
    api(projects.entryInteractions.spi)
    api(projects.domain)
    api(libs.kotlinx.coroutines.core)

    implementation(projects.core.common)
    implementation(projects.core.metro)
    implementation(projects.i18n)
    implementation(projects.icons.materialSymbols)
    implementation(projects.entryViewerChromeUi)
    implementation(projects.presentationCore)
    implementation(projects.sourceApi)
    implementation(projects.videoEngine.spi)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.appCompat)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.uiToolingPreview)
    implementation(libs.androidx.core)
    implementation(libs.androidx.media3.datasourceOkhttp)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayerHls)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.ui)
    implementation(libs.bundles.androidx.lifecycle)
    implementation(libs.bundles.serialization)
    api(libs.metrox.viewmodel)
    implementation(libs.unifile)

    testImplementation(projects.entryInteractions)
    testImplementation(libs.bundles.test)
    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.junit.vintage)
}

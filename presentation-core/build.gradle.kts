plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.compose)

    alias(mihonx.plugins.spotless)
    alias(libs.plugins.metro)
}

android {
    namespace = "tachiyomi.presentation.core"
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi",
            "-opt-in=androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
            "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi",
            "-opt-in=kotlinx.coroutines.FlowPreview",
        )
    }
}

dependencies {
    api(projects.core.common)
    implementation(projects.core.metro)
    implementation(projects.domain)
    implementation(projects.icons.materialSymbols)
    implementation(projects.icons.customMaterialSymbols)
    api(projects.i18n)

    // Compose
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation)
    // Kit types such as SnackbarHostState alias Material's; the UI kit boundary check keeps other modules off it.
    api(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.animationGraphics)
    debugImplementation(libs.androidx.compose.uiTooling)
    implementation(libs.androidx.compose.uiToolingPreview)
    implementation(libs.androidx.compose.uiUtil)
    implementation(libs.coil.compose)
    // Only kit/widget uses Glance, and only the widget module, which ships Glance, calls it.
    compileOnly(libs.androidx.glance.appWidget)

    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)
    implementation(libs.materialKolor)
    implementation(libs.composeRichEditor)
    implementation(libs.swipe)
}

import app.katari.gradle.extensions.desktop

plugins {
    alias(katari.plugins.kotlin.multiplatform)
    alias(katari.plugins.compose)

    alias(katari.plugins.spotless)
    alias(libs.plugins.metro)
}

kotlin {
    android {
        namespace = "tachiyomi.presentation.core"
        androidResources {
            enable = true
        }

        // TODO(antsy): Remove when https://youtrack.jetbrains.com/issue/KT-83319 is resolved
        withHostTest { }
    }
    desktop()

    sourceSets {
        commonMain {
            dependencies {
                api(projects.core.common)
                implementation(projects.core.metro)
                implementation(projects.core.domain)
                implementation(projects.core.icons.materialSymbols)
                implementation(projects.core.icons.customMaterialSymbols)
                api(projects.core.i18n)

                // Compose
                implementation(libs.compose.multiplatform.foundation)
                // Kit types such as SnackbarHostState alias Material's; the UI kit boundary check keeps other modules
                // off it.
                api(libs.compose.multiplatform.material3)
                implementation(libs.compose.multiplatform.animation)
                implementation(libs.compose.multiplatform.animationGraphics)
                implementation(libs.compose.multiplatform.uiUtil)
                implementation(libs.coil.compose)
                implementation(libs.androidx.annotation)

                implementation(libs.androidx.paging.compose)
                implementation(libs.materialKolor)
                implementation(libs.composeRichEditor)
                implementation(libs.swipe)
                // QR codes are drawn from the module grid ZXing encodes.
                implementation(libs.zxing.core)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.bundles.test)
                implementation(libs.kotlinx.coroutines.test)
                runtimeOnly(libs.junit.platform.launcher)
            }
        }
        androidMain {
            dependencies {
                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.window)
                implementation(libs.androidx.paging.runtime)
                // Only kit/widget uses Glance, and only the widget module, which ships Glance, calls it.
                compileOnly(libs.androidx.glance.appWidget)
            }
        }
        getByName("desktopMain") {
            dependencies {
                implementation(libs.compose.multiplatform.navigationEvent)
            }
        }
    }

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
            "-Xexpect-actual-classes",
        )
    }
}

import mihon.gradle.extensions.desktop

plugins {
    alias(mihonx.plugins.kotlin.multiplatform)
    alias(mihonx.plugins.spotless)

    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.metro)
}

kotlin {
    android {
        namespace = "tachiyomi.domain"

        // TODO(antsy): Remove when https://youtrack.jetbrains.com/issue/KT-83319 is resolved
        withHostTest { }
    }
    desktop()

    sourceSets {
        commonMain {
            dependencies {
                api(projects.i18n)

                implementation(projects.entrySourceApi)
                implementation(projects.sourceApi)
                implementation(projects.core.common)

                implementation(libs.bundles.kotlinx.coroutines)
                implementation(libs.bundles.serialization)
                implementation(libs.stringSimilarity)

                implementation(libs.kotlinx.datetime)

                api(libs.sqldelight.androidxPaging)

                compileOnly(project.dependencies.platform(libs.androidx.compose.bom))
                compileOnly(libs.androidx.compose.runtimeAnnotation)
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
                implementation(libs.unifile)
            }
        }
        getByName("desktopMain") {
            dependencies {
                implementation(libs.icu4j)
            }
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi")
    }
}

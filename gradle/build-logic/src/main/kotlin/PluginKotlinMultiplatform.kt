import app.katari.gradle.configurations.configureKotlin
import app.katari.gradle.configurations.configureMultiplatformTestVerification
import app.katari.gradle.extensions.alias
import app.katari.gradle.extensions.compileSdkFromCatalog
import app.katari.gradle.extensions.configureTest
import app.katari.gradle.extensions.coreLibraryDesugaring
import app.katari.gradle.extensions.katari
import app.katari.gradle.extensions.libs
import app.katari.gradle.extensions.plugins
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

@Suppress("UNUSED")
class PluginKotlinMultiplatform : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        plugins {
            alias(libs.plugins.android.kmp.library)
            alias(libs.plugins.kotlin.multiplatform)
        }

        configureKotlin()
        configureTest()
        configureMultiplatformTestVerification()

        kotlin {
            @OptIn(ExperimentalKotlinGradlePluginApi::class)
            applyDefaultHierarchyTemplate()

            android {
                minSdk = katari.versions.android.sdk.min.get().toInt()
                compileSdk(compileSdkFromCatalog())
                enableCoreLibraryDesugaring = true
            }
        }

        dependencies {
            coreLibraryDesugaring(libs.android.desugar)
        }
    }
}

private fun Project.kotlin(block: KotlinMultiplatformExtension.() -> Unit) {
    extensions.configure(block)
}

private fun KotlinMultiplatformExtension.android(block: KotlinMultiplatformAndroidLibraryTarget.() -> Unit) {
    targets.withType<KotlinMultiplatformAndroidLibraryTarget>().configureEach(block)
}

import com.android.build.api.dsl.BuildFeatures
import com.android.build.api.dsl.CommonExtension
import mihon.gradle.extensions.alias
import mihon.gradle.extensions.android
import mihon.gradle.extensions.api
import mihon.gradle.extensions.debugApi
import mihon.gradle.extensions.implementation
import mihon.gradle.extensions.libs
import mihon.gradle.extensions.plugins
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Compose for Android libraries and multiplatform modules alike. Multiplatform modules compile shared code against
 * JetBrains' Compose artifacts; their Android variants resolve to the androidx artifacts, which the BOM pins to the
 * versions Android-only modules use, so both kinds of module run the same Compose on Android.
 */
@Suppress("UNUSED")
class PluginCompose : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        plugins {
            alias(libs.plugins.kotlin.compose.compiler)
        }

        pluginManager.withPlugin(libs.plugins.kotlin.multiplatform.get().pluginId) {
            plugins {
                alias(libs.plugins.compose.multiplatform)
            }
            dependencies {
                add("androidMainImplementation", platform(libs.androidx.compose.bom))
                add("commonMainApi", libs.compose.multiplatform.uiToolingPreview)
            }
        }

        listOf(libs.plugins.android.library, libs.plugins.android.application).forEach { plugin ->
            pluginManager.withPlugin(plugin.get().pluginId) { configureAndroidCompose() }
        }
    }
}

private fun Project.configureAndroidCompose() {
    android {
        buildFeatures {
            compose = true
        }
    }

    dependencies {
        implementation(platform(libs.androidx.compose.bom))

        // Compose @Preview tooling
        api(libs.androidx.compose.uiToolingPreview)
        debugApi(libs.androidx.compose.uiTooling)
    }
}

private fun CommonExtension.buildFeatures(block: BuildFeatures.() -> Unit) {
    buildFeatures.apply(block)
}

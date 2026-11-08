import app.katari.gradle.configurations.configureAndroidTestVerification
import app.katari.gradle.configurations.configureKotlin
import app.katari.gradle.extensions.android
import app.katari.gradle.extensions.compileSdkFromCatalog
import app.katari.gradle.extensions.configureTest
import app.katari.gradle.extensions.coreLibraryDesugaring
import app.katari.gradle.extensions.katari
import app.katari.gradle.extensions.libs
import com.android.build.api.dsl.ApplicationDefaultConfig
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.CompileOptions
import com.android.build.api.dsl.DefaultConfig
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

@Suppress("UNUSED")
class PluginAndroidBase : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        configureKotlin()
        configureTest()
        configureAndroidTestVerification()

        android {
            defaultConfig {
                minSdk = katari.versions.android.sdk.min.get().toInt()
                if (this is ApplicationDefaultConfig) {
                    targetSdk = katari.versions.android.sdk.target.get().toInt()
                }

                ndkVersion = katari.versions.android.ndk.get()
            }

            compileSdk(compileSdkFromCatalog())

            compileOptions {
                isCoreLibraryDesugaringEnabled = true
            }
        }

        dependencies {
            coreLibraryDesugaring(libs.android.desugar)
        }
    }
}

private fun CommonExtension.defaultConfig(block: DefaultConfig.() -> Unit) {
    defaultConfig.apply(block)
}

private fun CommonExtension.compileOptions(block: CompileOptions.() -> Unit) {
    compileOptions.apply(block)
}

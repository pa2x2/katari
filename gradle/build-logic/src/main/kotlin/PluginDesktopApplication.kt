import app.katari.gradle.configurations.configureJvmTestVerification
import app.katari.gradle.configurations.configureKotlin
import app.katari.gradle.configurations.verifyDesktopMetroGraph
import app.katari.gradle.extensions.alias
import app.katari.gradle.extensions.configureTest
import app.katari.gradle.extensions.libs
import app.katari.gradle.extensions.plugins
import app.katari.gradle.jetbrainsruntime.runOnJetBrainsRuntime
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.desktop.DesktopExtension

@Suppress("UNUSED")
class PluginDesktopApplication : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        plugins {
            alias(libs.plugins.kotlin.jvm)
            alias(libs.plugins.kotlin.compose.compiler)
            alias(libs.plugins.compose.multiplatform)
        }

        configureKotlin()
        configureTest()
        configureJvmTestVerification()
        verifyDesktopMetroGraph()

        val compose = extensions.getByType<ComposeExtension>()
        (compose as ExtensionAware).extensions.getByType<DesktopExtension>().application {
            // Skia's native loader calls System.load; JDK 24+ warns about it, and will refuse it, without this flag.
            jvmArgs += "--enable-native-access=ALL-UNNAMED"
            // The X11 toolkit's window class, which the app names after its desktop entry (X11WindowClass)
            jvmArgs += "--add-opens=java.desktop/sun.awt.X11=ALL-UNNAMED"
            runOnJetBrainsRuntime(this)
        }
    }
}

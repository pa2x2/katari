import mihon.gradle.configurations.configureJvmTestVerification
import mihon.gradle.configurations.configureKotlin
import mihon.gradle.extensions.alias
import mihon.gradle.extensions.configureTest
import mihon.gradle.extensions.libs
import mihon.gradle.extensions.plugins
import mihon.gradle.jetbrainsruntime.runOnJetBrainsRuntime
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

        val compose = extensions.getByType<ComposeExtension>()
        (compose as ExtensionAware).extensions.getByType<DesktopExtension>().application {
            // Skia's native loader calls System.load; JDK 24+ warns about it, and will refuse it, without this flag.
            jvmArgs += "--enable-native-access=ALL-UNNAMED"
            // The X11 toolkit's window class, which the app names after its desktop entry (X11WindowClass)
            jvmArgs += "--add-opens=java.desktop/sun.awt.X11=ALL-UNNAMED"
            // The native Wayland toolkit's screen scale, which videos render at (the video engine's RenderScale)
            jvmArgs += "--add-opens=java.desktop/sun.awt.wl=ALL-UNNAMED"
            runOnJetBrainsRuntime(this)
        }
    }
}

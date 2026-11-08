import app.katari.gradle.extensions.alias
import app.katari.gradle.extensions.katari
import app.katari.gradle.extensions.libs
import app.katari.gradle.extensions.plugins
import org.gradle.api.Plugin
import org.gradle.api.Project

@Suppress("UNUSED")
class PluginAndroidTest : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        plugins {
            alias(libs.plugins.android.test)
            alias(katari.plugins.android.base)
        }
    }
}

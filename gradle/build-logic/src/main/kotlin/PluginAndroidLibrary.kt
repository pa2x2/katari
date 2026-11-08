import app.katari.gradle.extensions.alias
import app.katari.gradle.extensions.katari
import app.katari.gradle.extensions.libs
import app.katari.gradle.extensions.plugins
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

@Suppress("UNUSED")
class PluginAndroidLibrary : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        plugins {
            alias(libs.plugins.android.library)
            alias(katari.plugins.android.base)
        }

        extensions.configure<LibraryExtension> {
            // The app's tested foss variant consumes the release variant of every library, so
            // testing libraries on release reuses that compilation instead of compiling each
            // library a second time as debug.
            testBuildType = "release"
        }
    }
}

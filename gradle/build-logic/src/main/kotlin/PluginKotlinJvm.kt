import app.katari.gradle.configurations.configureJvmTestVerification
import app.katari.gradle.configurations.configureKotlin
import app.katari.gradle.extensions.alias
import app.katari.gradle.extensions.configureTest
import app.katari.gradle.extensions.libs
import app.katari.gradle.extensions.plugins
import org.gradle.api.Plugin
import org.gradle.api.Project

/** Desktop-only JVM libraries. Java sources in `src/main/java` compile beside the Kotlin ones. */
@Suppress("UNUSED")
class PluginKotlinJvm : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        plugins {
            alias(libs.plugins.kotlin.jvm)
        }

        configureKotlin()
        configureTest()
        configureJvmTestVerification()
    }
}

import mihon.gradle.configurations.configureJvmTestVerification
import mihon.gradle.configurations.configureKotlin
import mihon.gradle.extensions.alias
import mihon.gradle.extensions.configureTest
import mihon.gradle.extensions.libs
import mihon.gradle.extensions.plugins
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

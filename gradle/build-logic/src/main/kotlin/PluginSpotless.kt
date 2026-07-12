import com.diffplug.gradle.spotless.SpotlessExtension
import mihon.gradle.extensions.alias
import mihon.gradle.extensions.libs
import mihon.gradle.extensions.plugins
import mihon.gradle.tasks.EntryInteractionBoundaryCheckTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register

@Suppress("UNUSED")
class PluginSpotless : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        plugins {
            alias(libs.plugins.spotless)
        }

        // Configuration should be synced with [/gradle/build-logic/build.gradle.kts]
        val ktlintVersion = libs.ktlint.bom.get().version
        spotless {
            kotlin {
                target("src/**/*.kt")
                ktlint(ktlintVersion)
                trimTrailingWhitespace()
                endWithNewline()
            }

            kotlinGradle {
                target("*.kts")
                ktlint(ktlintVersion)
                trimTrailingWhitespace()
                endWithNewline()
            }

            format("xml") {
                target("src/**/*.xml")
                trimTrailingWhitespace()
                endWithNewline()
            }
        }

        if (this == rootProject) {
            val boundaryCheck = tasks.register<EntryInteractionBoundaryCheckTask>("checkEntryInteractionBoundaries") {
                description = "Checks Entry interaction boundary hardening rules."
                group = "verification"
                repositoryRoot.set(layout.projectDirectory)
            }

            tasks.named("spotlessCheck") {
                dependsOn(boundaryCheck)
            }
        }
    }
}

private fun Project.spotless(block: SpotlessExtension.() -> Unit) {
    extensions.configure(block)
}

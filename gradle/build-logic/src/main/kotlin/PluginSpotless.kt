import com.diffplug.gradle.spotless.SpotlessExtension
import mihon.gradle.extensioncompatibility.CheckReadmeExtensionTableTask
import mihon.gradle.extensioncompatibility.UpdateReadmeExtensionTableTask
import mihon.gradle.extensions.alias
import mihon.gradle.extensions.libs
import mihon.gradle.extensions.plugins
import mihon.gradle.tasks.EntryInteractionBoundaryCheckTask
import mihon.gradle.tasks.UiKitBoundaryCheckTask
import mihon.gradle.tasks.VendoredScriptsCheckTask
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
                forbidRegex(
                    "materialSymbolsRImport",
                    "(?m)^import mihon\\.icons\\.materialsymbols\\.R(?! as MaterialSymbolsR$)\\b.*$",
                    "Import mihon.icons.materialsymbols.R as MaterialSymbolsR",
                )
                forbidRegex(
                    "materialSymbolsRQualified",
                    "(?m)^(?!import ).*\\bmihon\\.icons\\.materialsymbols\\.R\\.",
                    "Use the MaterialSymbolsR import alias instead of the qualified name",
                )
                forbidRegex(
                    "customMaterialSymbolsRImport",
                    "(?m)^import mihon\\.icons\\.custommaterialsymbols\\.R(?! as CustomMaterialSymbolsR$)\\b.*$",
                    "Import mihon.icons.custommaterialsymbols.R as CustomMaterialSymbolsR",
                )
                forbidRegex(
                    "customMaterialSymbolsRQualified",
                    "(?m)^(?!import ).*\\bmihon\\.icons\\.custommaterialsymbols\\.R\\.",
                    "Use the CustomMaterialSymbolsR import alias instead of the qualified name",
                )
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

        if (path == ":") {
            val boundaryCheck = tasks.register<EntryInteractionBoundaryCheckTask>("checkEntryInteractionBoundaries") {
                description = "Checks Entry and application Feature boundary hardening rules."
                group = "verification"
                repositoryRoot.set(layout.projectDirectory)
            }
            val uiKitBoundaryCheck = tasks.register<UiKitBoundaryCheckTask>("checkUiKitBoundaries") {
                description = "Checks that UI is built only from presentation-core kit components and tokens."
                group = "verification"
                repositoryRoot.set(layout.projectDirectory)
                report.set(layout.buildDirectory.file("reports/ui-kit-boundaries.txt"))
            }
            val featureBoundaryCheck = tasks.register("checkFeatureArchitectureBoundaries") {
                description = "Checks all Feature architecture boundary hardening rules."
                group = "verification"
                dependsOn(boundaryCheck, uiKitBoundaryCheck)
            }

            val extensionFamilies = layout.projectDirectory.file("app/extension-compatibility.json")
            val updateReadmeExtensionTable = tasks.register<UpdateReadmeExtensionTableTask>(
                "updateReadmeExtensionTable",
            ) {
                description = "Writes README.md's table of the extension families the app loads."
                source.set(extensionFamilies)
                readme.set(layout.projectDirectory.file("README.md"))
            }
            val checkReadmeExtensionTable = tasks.register<CheckReadmeExtensionTableTask>("checkReadmeExtensionTable") {
                description = "Checks that README.md's table of extension families matches what the app loads."
                group = "verification"
                source.set(extensionFamilies)
                readme.set(layout.projectDirectory.file("README.md"))
            }
            val checkVendoredScripts = tasks.register<VendoredScriptsCheckTask>("checkVendoredScripts") {
                description = "Checks that committed script bundles match the SHA256SUMS their npm build wrote."
                group = "verification"
                manifests.from(fileTree(layout.projectDirectory) { include("*/*bundle/SHA256SUMS") })
            }
            tasks.named("spotlessApply") {
                dependsOn(updateReadmeExtensionTable)
            }
            tasks.named("spotlessCheck") {
                dependsOn(featureBoundaryCheck, checkReadmeExtensionTable, checkVendoredScripts)
            }
        }
    }
}

private fun Project.spotless(block: SpotlessExtension.() -> Unit) {
    extensions.configure(block)
}

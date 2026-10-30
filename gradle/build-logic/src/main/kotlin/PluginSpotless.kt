import com.diffplug.gradle.spotless.SpotlessExtension
import mihon.gradle.extensioncompatibility.CheckReadmeExtensionTableTask
import mihon.gradle.extensioncompatibility.UpdateReadmeExtensionTableTask
import mihon.gradle.extensions.alias
import mihon.gradle.extensions.libs
import mihon.gradle.extensions.plugins
import mihon.gradle.platformparity.ANDROID_PLATFORM_API_LIBRARIES
import mihon.gradle.platformparity.HostParityCheckTask
import mihon.gradle.platformparity.PLATFORM_API_LIBRARIES_ATTRIBUTE
import mihon.gradle.platformparity.PlatformApiIndexTask
import mihon.gradle.platformparity.PlatformFacilityCheckTask
import mihon.gradle.platformparity.PlatformPlacementCheckTask
import mihon.gradle.tasks.EntryInteractionBoundaryCheckTask
import mihon.gradle.tasks.MetroContributionVisibilityCheckTask
import mihon.gradle.tasks.UiKitBoundaryCheckTask
import mihon.gradle.tasks.VendoredScriptsCheckTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.java.TargetJvmEnvironment
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskProvider
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
            val metroContributionCheck = tasks.register<MetroContributionVisibilityCheckTask>(
                "checkMetroContributionVisibility",
            ) {
                description = "Checks that Metro contributions outside the graph modules are public."
                group = "verification"
                repositoryRoot.set(layout.projectDirectory)
            }
            val hostParityCheck = tasks.register<HostParityCheckTask>("checkHostParity") {
                description = "Checks that the Android and desktop graphs compose shared code the same way."
                group = "verification"
                repositoryRoot.set(layout.projectDirectory)
            }
            val platformApiIndex = registerPlatformApiIndex()
            val platformPlacementCheck = tasks.register<PlatformPlacementCheckTask>("checkPlatformPlacement") {
                description = "Checks that platform-only source sets hold only code that needs their platform."
                group = "verification"
                repositoryRoot.set(layout.projectDirectory)
                platformApis.set(platformApiIndex.flatMap { it.index })
            }
            val platformFacilityCheck = tasks.register<PlatformFacilityCheckTask>("checkPlatformFacilities") {
                description =
                    "Checks that platforms implement shared contracts themselves only for platform facilities."
                group = "verification"
                repositoryRoot.set(layout.projectDirectory)
                platformApis.set(platformApiIndex.flatMap { it.index })
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
                dependsOn(
                    featureBoundaryCheck,
                    metroContributionCheck,
                    hostParityCheck,
                    platformPlacementCheck,
                    platformFacilityCheck,
                    checkReadmeExtensionTable,
                    checkVendoredScripts,
                )
            }
        }
    }
}

/**
 * Registers `indexPlatformApis` over the libraries the root build script hands it: `platformApiDesktop` takes the
 * desktop app and the libraries its runtime carries, `platformApiAndroid` the Android app (which exposes its libraries
 * through `exposeAndroidPlatformApiLibraries`) and `platformApiAndroidStandIns` the module of the Android API's
 * desktop stand-ins.
 */
private fun Project.registerPlatformApiIndex(): TaskProvider<PlatformApiIndexTask> {
    val desktop = jvmLibraries(resolvableFrom("platformApiDesktop"), externalOnly = true)
    val android = resolvableFrom("platformApiAndroid").apply {
        attributes.attribute(PLATFORM_API_LIBRARIES_ATTRIBUTE, ANDROID_PLATFORM_API_LIBRARIES)
    }
    val standIns = jvmLibraries(resolvableFrom("platformApiAndroidStandIns").apply { isTransitive = false }, false)
    return tasks.register<PlatformApiIndexTask>("indexPlatformApis") {
        description = "Indexes the names outside the repository that desktop and Android code can use."
        group = "verification"
        desktopLibraries.from(desktop)
        desktopJdkVersion.set(providers.systemProperty("java.version"))
        androidLibraryNames.from(android)
        androidStandIns.from(standIns)
        index.set(layout.buildDirectory.file("platform-apis/index.txt"))
    }
}

private fun Project.resolvableFrom(dependencyScope: String): Configuration {
    val dependencies = configurations.dependencyScope(dependencyScope)
    return configurations.resolvable("${dependencyScope}Classpath") { extendsFrom(dependencies.get()) }.get()
}

/** The jars of [configuration] as a desktop JVM compilation resolves them; only external ones when [externalOnly]. */
private fun Project.jvmLibraries(configuration: Configuration, externalOnly: Boolean): FileCollection {
    configuration.attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, Usage.JAVA_RUNTIME))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category::class.java, Category.LIBRARY))
        attribute(
            LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
            objects.named(LibraryElements::class.java, LibraryElements.JAR),
        )
        attribute(
            TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
            objects.named(TargetJvmEnvironment::class.java, TargetJvmEnvironment.STANDARD_JVM),
        )
    }
    if (!externalOnly) return configuration
    return configuration.incoming.artifactView { componentFilter { it is ModuleComponentIdentifier } }.files
}

private fun Project.spotless(block: SpotlessExtension.() -> Unit) {
    extensions.configure(block)
}

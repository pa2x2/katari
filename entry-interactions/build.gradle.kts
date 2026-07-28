import mihon.gradle.tasks.GenerateEntryInteractionTopologyTask
import org.gradle.api.tasks.testing.Test

plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.spotless)
}

android {
    namespace = "mihon.entry.interactions"

    testFixtures {
        enable = true
    }
}

androidComponents {
    onVariants { variant ->
        val javaSources = variant.sources.java ?: return@onVariants
        val variantName = variant.name.replaceFirstChar { it.uppercase() }
        val generatedTopology = tasks.register<GenerateEntryInteractionTopologyTask>(
            "generate${variantName}EntryInteractionTopology",
        ) {
            this.variantName.set(variant.name)
            featureModuleDescriptors.from(
                fileTree(layout.projectDirectory.dir("src/main")) {
                    include("**/*.entry-feature-module")
                },
                fileTree(layout.projectDirectory.dir("src/${variant.name}")) {
                    include("**/*.entry-feature-module")
                },
            )
            typeModuleDescriptors.from(
                rootProject.fileTree("entry-interactions") {
                    include("*/src/main/**/*.entry-type-module")
                    include("*/src/${variant.name}/**/*.entry-type-module")
                },
            )
            outputDirectory.set(
                layout.buildDirectory.dir("generated/source/entry-interaction-topology/${variant.name}"),
            )
        }
        javaSources.addGeneratedSourceDirectory(generatedTopology) { it.outputDirectory }
    }
}

dependencies {
    api(projects.entryInteractions.api)
    api(projects.entryViewerSettingsApi)

    implementation(projects.core.common)
    implementation(projects.entryInteractions.spi)
    implementation(projects.entryInteractions.downloadNotification)
    implementation(projects.entryInteractions.manga)
    implementation(projects.entryInteractions.anime)
    implementation(projects.entryInteractions.book)
    implementation(projects.sourceCompat)
    implementation(projects.sourceLocal)
    implementation(libs.androidx.core)
    implementation(libs.androidx.work)
    implementation(libs.injekt)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.bundles.serialization)
    implementation(libs.bundles.coil)

    testImplementation(libs.bundles.test)
    testImplementation(libs.bundles.serialization)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.unifile)
    testImplementation(projects.featureValidation)
    testRuntimeOnly(libs.junit.platform.launcher)

    testFixturesImplementation(libs.bundles.test)
    testFixturesImplementation(libs.bundles.serialization)
    testFixturesImplementation(libs.kotlinx.coroutines.test)
    testFixturesImplementation(libs.unifile)
    testFixturesImplementation(platform(libs.androidx.compose.bom))
    testFixturesApi(projects.entryInteractions.spi)
}

val featureReportFile = layout.buildDirectory.file("reports/features/developer-report.txt")

val generateFeatureReport = tasks.register<Test>("generateFeatureReport") {
    group = "reporting"
    description = "Renders the evaluated application and Entry Feature graph for developers"

    testClassesDirs = files(
        providers.provider { tasks.named<Test>("testDebugUnitTest").get().testClassesDirs },
    )
    classpath = files(
        providers.provider { tasks.named<Test>("testDebugUnitTest").get().classpath },
    )
    filter {
        includeTestsMatching(
            "mihon.entry.interactions.validation.ProductionEntryInteractionDeveloperReportTest",
        )
    }
    systemProperty(
        "mihon.feature.report.output",
        featureReportFile.get().asFile.absolutePath,
    )
    outputs.file(featureReportFile)
    testLogging.showStandardStreams = true
}

tasks.register("generateEntryFeatureReport") {
    group = "reporting"
    description = "Compatibility alias for generateFeatureReport"
    dependsOn(generateFeatureReport)
}

import mihon.gradle.tasks.GenerateEntryInteractionTopologyTask

plugins {
    alias(mihonx.plugins.android.library)
    alias(mihonx.plugins.spotless)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.metro)
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
            typeModuleDescriptors.from(
                fileTree(layout.projectDirectory) {
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
    implementation(projects.core.metro)
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
    testImplementation(libs.androidx.work.testing)
    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
    testImplementation(projects.featureValidation)
    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.junit.vintage)

    testFixturesImplementation(libs.bundles.test)
    testFixturesImplementation(libs.bundles.serialization)
    testFixturesImplementation(libs.kotlinx.coroutines.test)
    testFixturesImplementation(libs.unifile)
    testFixturesImplementation(projects.featureValidation)
    testFixturesImplementation(projects.core.metro)
    testFixturesImplementation(platform(libs.androidx.compose.bom))
    testFixturesApi(projects.entryInteractions.spi)
}

val generateFeatureReport = tasks.register("generateFeatureReport") {
    group = "reporting"
    description = "Compatibility alias for :app:generateFeatureReport"
    dependsOn(":app:generateFeatureReport")
}

tasks.register("generateEntryFeatureReport") {
    group = "reporting"
    description = "Compatibility alias for generateFeatureReport"
    dependsOn(generateFeatureReport)
}

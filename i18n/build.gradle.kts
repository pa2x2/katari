import mihon.gradle.extensions.desktop
import mihon.gradle.tasks.GenerateAppLocaleTagsTask
import mihon.gradle.tasks.GenerateLocalesConfigTask
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(mihonx.plugins.kotlin.multiplatform)
    alias(mihonx.plugins.spotless)

    alias(libs.plugins.moko.resources)
}

// Each language's strings, from which its language tag is read
val stringFiles = layout.projectDirectory.dir("src/commonMain/moko-resources").asFileTree.matching {
    include("**/strings.xml", "**/plurals.xml")
}

val generateAppLocaleTags = tasks.register<GenerateAppLocaleTagsTask>("generateAppLocaleTags") {
    localeFiles.from(stringFiles)
    packageName.set("tachiyomi.i18n")
    outputDir.set(layout.buildDirectory.dir("generated/appLocaleTags"))
}

kotlin {
    android {
        namespace = "tachiyomi.i18n"

        // TODO(antsy): Remove when https://youtrack.jetbrains.com/issue/KT-83319 is resolved
        withHostTest { }
    }
    desktop()

    sourceSets.commonMain {
        kotlin.srcDir(generateAppLocaleTags.flatMap { it.outputDir })
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    dependencies {
        api(libs.moko.resources)
        api(projects.core.platform)
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}

androidComponents {
    onVariants { variant ->
        val resSource = variant.sources.res ?: return@onVariants

        val variantName = variant.name.replaceFirstChar { it.uppercase() }
        val task = tasks.register<GenerateLocalesConfigTask>("generate${variantName}LocalesConfig") {
            localeFiles.from(stringFiles)
        }
        resSource.addGeneratedSourceDirectory(task) { it.outputDir }
    }
}

multiplatformResources {
    resourcesPackage.set("tachiyomi.i18n")
}

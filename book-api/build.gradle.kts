import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(mihonx.plugins.kotlin.multiplatform)
    alias(mihonx.plugins.spotless)
    alias(libs.plugins.dokka)
    alias(libs.plugins.kotlin.serialization)
    id("maven-publish")
}

group = "com.github.katariapp.katari"
version = providers.gradleProperty("sourceApiVersion")
    .orElse(System.getenv("VERSION") ?: "local-SNAPSHOT")
    .get()

dokka {
    moduleName.set("Katari Book API")
    moduleVersion.set(project.version.toString())

    dokkaPublications.html {
        failOnWarning.set(true)
        suppressInheritedMembers.set(false)
        suppressObviousFunctions.set(true)
    }

    dokkaSourceSets.configureEach {
        documentedVisibilities.set(setOf(VisibilityModifier.Public))
        reportUndocumented.set(true)
        skipEmptyPackages.set(true)
        sourceLink {
            localDirectory.set(rootProject.projectDir)
            remoteUrl.set(uri("https://github.com/katariapp/katari/blob/main"))
            remoteLineSuffix.set("#L")
        }
    }
}

kotlin {
    @Suppress("UnstableApiUsage")
    android {
        namespace = "mihon.book.api"

        // TODO(antsy): Remove when https://youtrack.jetbrains.com/issue/KT-83319 is resolved
        withHostTest { }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    dependencies {
        api(libs.kotlinx.serialization.json)
    }

    sourceSets {
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

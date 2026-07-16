plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.samWithReceiver)
    alias(libs.plugins.spotless)
    `java-gradle-plugin`
}

// Configuration should be synced with [/gradle/build-logic/src/main/kotlin/PluginSpotless.kt]
val ktlintVersion = libs.ktlint.bom.get().version
val editorConfigFile = rootProject.file("../../.editorconfig")
spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint(ktlintVersion).setEditorConfigPath(editorConfigFile)
        trimTrailingWhitespace()
        endWithNewline()
    }

    kotlinGradle {
        target("*.kts")
        ktlint(ktlintVersion).setEditorConfigPath(editorConfigFile)
        trimTrailingWhitespace()
        endWithNewline()
    }
}

dependencies {
    compileOnly(gradleKotlinDsl())
    compileOnly(libs.android.gradle)
    compileOnly(libs.kotlin.compose.compiler.gradle)
    compileOnly(libs.kotlin.gradle)
    implementation(libs.spotless.gradle)
    implementation(libs.tapmoc.gradle)

    // These allow us to reference the dependency catalog inside our compiled plugins
    compileOnly(files(libs::class.java.superclass.protectionDomain.codeSource.location))
    compileOnly(files(mihonx::class.java.superclass.protectionDomain.codeSource.location))

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotest.assertions)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

samWithReceiver {
    annotation("org.gradle.api.HasImplicitReceiver")
}

gradlePlugin {
    plugins {
        register("artifact-sanitizer") {
            id = mihonx.plugins.artifact.sanitizer.get().pluginId
            implementationClass = "PluginArtifactSanitizer"
        }
        register("android-application") {
            id = mihonx.plugins.android.application.get().pluginId
            implementationClass = "PluginAndroidApplication"
        }
        register("android-base") {
            id = mihonx.plugins.android.base.get().pluginId
            implementationClass = "PluginAndroidBase"
        }
        register("android-library") {
            id = mihonx.plugins.android.library.get().pluginId
            implementationClass = "PluginAndroidLibrary"
        }
        register("android-test") {
            id = mihonx.plugins.android.test.get().pluginId
            implementationClass = "PluginAndroidTest"
        }
        register("compose-android") {
            id = mihonx.plugins.compose.get().pluginId
            implementationClass = "PluginComposeAndroid"
        }
        register("kotlin-multiplatform") {
            id = mihonx.plugins.kotlin.multiplatform.get().pluginId
            implementationClass = "PluginKotlinMultiplatform"
        }
        register("readium-navigator") {
            id = mihonx.plugins.readium.navigator.get().pluginId
            implementationClass = "PluginReadiumNavigator"
        }
        register("spotless") {
            id = mihonx.plugins.spotless.get().pluginId
            implementationClass = "PluginSpotless"
        }
    }
}

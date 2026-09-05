import mihon.gradle.Config
import mihon.gradle.getBuildTime
import mihon.gradle.getLatestCommitCount
import mihon.gradle.getLatestCommitSha
import mihon.gradle.tasks.GenerateApplicationFeatureTopologyTask
import mihon.gradle.tasks.ReplaceShortcutsPlaceholderTask
import org.gradle.api.tasks.testing.Test
import java.io.FileInputStream
import java.util.Properties
import kotlin.io.encoding.Base64

plugins {
    alias(mihonx.plugins.android.application)
    alias(mihonx.plugins.artifact.sanitizer)
    alias(mihonx.plugins.compose)
    alias(mihonx.plugins.spotless)

    alias(libs.plugins.aboutLibraries)
    alias(libs.plugins.androidx.baselineProfile)
    alias(libs.plugins.kotlin.serialization)
}

if (Config.includeTelemetry) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
    apply(plugin = libs.plugins.firebase.crashlytics.get().pluginId)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")

android {
    namespace = "eu.kanade.tachiyomi"

    defaultConfig {
        applicationId = "app.katari"

        versionCode = 56
        versionName = "1.8.0"

        buildConfigField("String", "COMMIT_COUNT", "\"${getLatestCommitCount()}\"")
        buildConfigField("String", "COMMIT_SHA", "\"${getLatestCommitSha()}\"")
        buildConfigField("String", "BUILD_TIME", "\"${getBuildTime(useLatestCommitTime = false)}\"")
        buildConfigField("boolean", "TELEMETRY_INCLUDED", "${Config.includeTelemetry}")
        buildConfigField("boolean", "UPDATER_ENABLED", "${Config.enableUpdater}")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    testBuildType = "foss"

    if (System.getenv("KATARI_GITHUB_RELEASE").toBoolean()) {
        val tempStoreFile = file(System.getenv("RUNNER_TEMP")).resolve("antsy.keystore")

        val storeFileBytes = System.getenv("storeFileBase64").let(Base64::decode)
        tempStoreFile.outputStream().use { it.write(storeFileBytes) }

        signingConfigs {
            named("debug") {
                storeFile = tempStoreFile
                storePassword = System.getenv("storePassword")
                keyAlias = System.getenv("keyAlias")
                keyPassword = System.getenv("keyPassword")
            }
        }
    } else if (keystorePropertiesFile.exists()) {
        val keystoreProperties = FileInputStream(keystorePropertiesFile).use { Properties().apply { load(it) } }

        signingConfigs {
            named("debug") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        val debug = getByName("debug") {
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-${getLatestCommitCount()}"
            isPseudoLocalesEnabled = true
        }
        val release = getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true

            signingConfig = debug.signingConfig

            isProfileable = true

            proguardFiles("proguard-android-optimize.txt", "proguard-rules.pro")

            buildConfigField("String", "BUILD_TIME", "\"${getBuildTime(useLatestCommitTime = true)}\"")
        }

        val commonMatchingFallbacks = listOf(release.name)

        create("foss") {
            initWith(release)

            applicationIdSuffix = ".foss"

            matchingFallbacks.addAll(commonMatchingFallbacks)
        }
        create("preview") {
            initWith(release)

            applicationIdSuffix = ".debug"

            versionNameSuffix = debug.versionNameSuffix

            matchingFallbacks.addAll(commonMatchingFallbacks)

            buildConfigField("String", "BUILD_TIME", "\"${getBuildTime(useLatestCommitTime = false)}\"")
        }
        create("benchmark") {
            initWith(release)

            versionNameSuffix = "-benchmark"
            applicationIdSuffix = ".benchmark"

            matchingFallbacks.addAll(commonMatchingFallbacks)
        }
    }

    sourceSets {
        getByName("preview").res.directories.add("src/debug/res")
        getByName("benchmark").res.directories.add("src/debug/res")
    }

    splits {
        abi {
            isEnable = true
            isUniversalApk = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
    }

    packaging {
        jniLibs {
            keepDebugSymbols += listOf(
                "libandroidx.graphics.path",
                "libarchive-jni",
                "libimagedecoder",
                "libquickjs",
                "libsqlite3x",
            )
                .map { "**/$it.so" }
        }
        resources {
            excludes += setOf(
                "kotlin-tooling-metadata.json",
                "LICENSE.txt",
                "META-INF/**/*.properties",
                "META-INF/**/LICENSE.txt",
                "META-INF/*.properties",
                "META-INF/*.version",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/NOTICE",
                "META-INF/README.md",
            )
        }
    }

    dependenciesInfo {
        includeInApk = Config.includeDependencyInfo
        includeInBundle = Config.includeDependencyInfo
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        aidl = true
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi",
            "-opt-in=androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
            "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi",
            "-opt-in=coil3.annotation.ExperimentalCoilApi",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.FlowPreview",
            "-opt-in=kotlinx.coroutines.InternalCoroutinesApi",
            "-opt-in=kotlinx.serialization.ExperimentalSerializationApi",
        )
    }
}

baselineProfile {
    baselineProfileOutputDir = "baselineProfiles"
    mergeIntoMain = true
}

dependencies {
    constraints {
        implementation(libs.androidx.concurrent.futures)
        implementation(libs.androidx.concurrent.futures.ktx)
    }

    baselineProfile(dependencies.project(mapOf("path" to projects.baselineProfile.path)))

    implementation(dependencies.project(mapOf("path" to projects.i18n.path)))
    implementation(dependencies.project(mapOf("path" to projects.core.archive.path)))
    implementation(dependencies.project(mapOf("path" to projects.core.common.path)))
    implementation(dependencies.project(mapOf("path" to projects.coreMetadata.path)))
    implementation(dependencies.project(mapOf("path" to projects.entryInteractions.path)))
    implementation(dependencies.project(mapOf("path" to projects.entrySourceApi.path)))
    implementation(dependencies.project(mapOf("path" to projects.featureRuntime.path)))
    implementation(dependencies.project(mapOf("path" to projects.sourceApi.path)))
    implementation(dependencies.project(mapOf("path" to projects.sourceCompat.path)))
    implementation(dependencies.project(mapOf("path" to projects.sourceLocal.path)))
    implementation(dependencies.project(mapOf("path" to projects.data.path)))
    implementation(dependencies.project(mapOf("path" to projects.domain.path)))
    implementation(dependencies.project(mapOf("path" to projects.presentationCore.path)))
    implementation(dependencies.project(mapOf("path" to projects.presentationWidget.path)))
    implementation(dependencies.project(mapOf("path" to projects.telemetry.path)))
    implementation(dependencies.project(mapOf("path" to projects.translation.runtime.path)))
    implementation(dependencies.project(mapOf("path" to projects.translation.ui.path)))
    implementation(dependencies.project(mapOf("path" to projects.translation.providers.libretranslate.path)))
    implementation(dependencies.project(mapOf("path" to projects.tts.runtime.path)))
    implementation(dependencies.project(mapOf("path" to projects.tts.providers.android.path)))
    implementation(dependencies.project(mapOf("path" to projects.tts.ui.path)))

    // Compose
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.materialIcons)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.animationGraphics)
    debugImplementation(libs.androidx.compose.uiTooling)
    implementation(libs.androidx.compose.uiToolingPreview)
    implementation(libs.androidx.compose.uiUtil)

    implementation(libs.androidx.interpolator)

    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    implementation(libs.androidx.sqlite.bundled)

    implementation(libs.kotlin.reflect)

    implementation(libs.bundles.kotlinx.coroutines)

    implementation(libs.sqldelight.async)

    implementation(libs.kotlinx.datetime)

    // AndroidX libraries
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.appCompat)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.constraintLayout)
    implementation(libs.androidx.core)
    implementation(libs.androidx.coreSplashScreen)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayerHls)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.datasourceOkhttp)
    implementation(libs.androidx.recyclerView)
    implementation(libs.androidx.viewPager)
    implementation(libs.androidx.profileInstaller)

    implementation(libs.bundles.androidx.lifecycle)

    // Job scheduling
    implementation(libs.androidx.work)

    // RxJava
    implementation(libs.rxJava)

    // Networking
    implementation(libs.bundles.okhttp)
    implementation(libs.okio)

    // Data serialization (JSON, protobuf, xml)
    implementation(libs.bundles.serialization)

    // HTML parser
    implementation(libs.jsoup)

    // Disk
    implementation(libs.diskLruCache)
    implementation(libs.unifile)

    // Preferences
    implementation(libs.androidx.preference)

    // Dependency injection
    implementation(libs.injekt)

    // Image loading
    implementation(libs.bundles.coil)
    implementation(libs.subsamplingScaleImageView) {
        exclude(module = "image-decoder")
    }
    implementation(libs.image.decoder)

    // UI libraries
    implementation(libs.material)
    implementation(libs.flexibleAdapter)
    implementation(libs.photoView)
    implementation(libs.directionalViewPager) {
        exclude(group = "androidx.viewpager", module = "viewpager")
    }
    implementation(libs.composeRichEditor)
    implementation(libs.aboutLibraries.compose)
    implementation(libs.bundles.voyager)
    implementation(libs.composeMaterialMotion)
    implementation(libs.swipe)
    implementation(libs.composeWebview)
    implementation(libs.composeGrid)
    implementation(libs.reorderable)
    implementation(libs.bundles.markdown)
    implementation(libs.materialKolor)

    // Logging
    implementation(libs.logcat)

    // Shizuku
    implementation(libs.bundles.shizuku)

    // String similarity
    implementation(libs.stringSimilarity)

    // Tests
    testImplementation(libs.bundles.test)
    testImplementation(projects.featureValidation)
    testImplementation(testFixtures(projects.entryInteractions))
    testImplementation(testFixtures(projects.translation.runtime))
    testRuntimeOnly(libs.junit.platform.launcher)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.uiTestJunit4)
    androidTestImplementation(libs.androidx.test.junit)
    debugImplementation(libs.androidx.compose.uiTestManifest)

    // For detecting memory leaks; see https://square.github.io/leakcanary/
    // debugImplementation(libs.leakCanary.android)
    implementation(libs.leakCanary.plumber)

    testImplementation(libs.kotlinx.coroutines.test)
}

val featureReportFile = layout.buildDirectory.file("reports/features/developer-report.txt")

val generateFeatureReport = tasks.register<Test>("generateFeatureReport") {
    group = "reporting"
    description = "Renders the evaluated application and Entry Feature graph for developers"

    testClassesDirs = files(
        providers.provider { tasks.named<Test>("testFossUnitTest").get().testClassesDirs },
    )
    classpath = files(
        providers.provider { tasks.named<Test>("testFossUnitTest").get().classpath },
    )
    filter {
        includeTestsMatching(
            "eu.kanade.tachiyomi.validation.ProductionFeatureDeveloperReportTest",
        )
    }
    systemProperty(
        "mihon.feature.report.output",
        featureReportFile.get().asFile.absolutePath,
    )
    outputs.file(featureReportFile)
    testLogging.showStandardStreams = true
}

androidComponents {
    onVariants { variant ->
        val javaSources = variant.sources.java
        if (javaSources != null) {
            val variantName = variant.name.replaceFirstChar { it.uppercase() }
            val generateApplicationFeatureTopology = tasks.register<GenerateApplicationFeatureTopologyTask>(
                "generate${variantName}ApplicationFeatureTopology",
            ) {
                this.variantName.set(variant.name)
                moduleDescriptors.from(
                    rootProject.fileTree(rootProject.layout.projectDirectory) {
                        include("**/src/main/**/*.application-feature-module")
                        include("**/src/${variant.name}/**/*.application-feature-module")
                        exclude("**/build/**")
                        exclude("translation-workspace/**")
                    },
                )
                componentDescriptors.from(
                    rootProject.fileTree(rootProject.layout.projectDirectory) {
                        include("**/src/main/**/*.application-feature-runtime-component")
                        include("**/src/${variant.name}/**/*.application-feature-runtime-component")
                        exclude("**/build/**")
                        exclude("translation-workspace/**")
                    },
                )
                outputDirectory.set(
                    layout.buildDirectory.dir("generated/source/application-feature-topology/${variant.name}"),
                )
            }
            javaSources.addGeneratedSourceDirectory(generateApplicationFeatureTopology) { it.outputDirectory }
        }

        if (Config.includeTelemetry) {
            variant.sources.manifests.addStaticManifestFile(
                projectDir.resolve("src/telemetry/AndroidManifest.xml").absolutePath,
            )
        }

        val resSource = variant.sources.res ?: return@onVariants

        val variantName = variant.name.replaceFirstChar { it.uppercase() }
        val replaceShortcutsPlaceholderTask = tasks.register<ReplaceShortcutsPlaceholderTask>(
            "replace${variantName}ShortcutPlaceholder",
        ) {
            applicationId.set(variant.applicationId)
            shortcutsFile.set(projectDir.resolve("src/main/shortcuts.xml"))
        }
        resSource.addGeneratedSourceDirectory(replaceShortcutsPlaceholderTask) { it.outputDir }
    }

    onVariants(selector().withFlavor("default" to "standard")) {
        // Only excluding in standard flavor because this breaks
        // Layout Inspector's Compose tree
        it.packaging.resources.excludes.add("META-INF/*.version")
    }
}

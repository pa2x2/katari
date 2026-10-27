import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.ApplicationVariant
import com.android.build.api.variant.BuildConfigField
import com.android.build.api.variant.FilterConfiguration
import mihon.gradle.Config
import mihon.gradle.extensioncompatibility.GenerateExtensionFamiliesTask
import mihon.gradle.getCurrentTime
import mihon.gradle.getLatestCommitCount
import mihon.gradle.getLatestCommitSha
import mihon.gradle.getLatestCommitTime
import mihon.gradle.licences.GenerateLibmpvLicencesTask
import mihon.gradle.tasks.CheckApkNativeLibrariesTask
import mihon.gradle.tasks.ReplaceShortcutsPlaceholderTask
import org.gradle.api.tasks.testing.Test
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(mihonx.plugins.android.application)
    alias(mihonx.plugins.artifact.sanitizer)
    alias(mihonx.plugins.compose)
    alias(mihonx.plugins.spotless)

    alias(libs.plugins.aboutLibraries)
    alias(libs.plugins.androidx.baselineProfile)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.apollo)
    alias(libs.plugins.metro)
}

if (Config.includeTelemetry) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
    apply(plugin = libs.plugins.firebase.crashlytics.get().pluginId)
}

val keystorePropertiesFile = layout.settingsDirectory.file("keystore.properties").asFile

/** ABIs of the published APKs; the in-app updater keeps its own copy in ReleaseApkSelection. */
val releasedAbis = listOf("armeabi-v7a", "arm64-v8a")

/** Development builds also include x86_64 so they run natively on emulators. */
val emulatorAbis = listOf("x86_64")

/** Build types whose APKs are published. */
val releasedBuildTypes = setOf("release", "foss")

android {
    namespace = "eu.kanade.tachiyomi"

    defaultConfig {
        applicationId = "app.katari"

        versionCode = 86
        versionName = "2.0.1"

        buildConfigField("boolean", "TELEMETRY_INCLUDED", "${Config.includeTelemetry}")
        buildConfigField("String", "MPV_ADDON_RELEASES_URL", "\"\"")
    }

    testBuildType = "foss"

    if (System.getenv("KATARI_GITHUB_RELEASE").toBoolean()) {
        signingConfigs {
            named("debug") {
                storeFile = file(System.getenv("storeFile"))
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
            isPseudoLocalesEnabled = true

            // GitHub's mpv add-on is signed with the release key, which a locally built Katari doesn't accept.
            // -PmpvAddonReleasesUrl=<URL of a GitHub-style releases list> serves a locally built one instead.
            val mpvAddonReleasesUrl = providers.gradleProperty("mpvAddonReleasesUrl").orNull.orEmpty()
            buildConfigField("String", "MPV_ADDON_RELEASES_URL", "\"$mpvAddonReleasesUrl\"")
        }
        val release = getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true

            signingConfig = debug.signingConfig

            isProfileable = true

            proguardFiles("proguard-android-optimize.txt", "proguard-rules.pro")
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

            matchingFallbacks.addAll(commonMatchingFallbacks)
        }
        create("benchmark") {
            initWith(release)

            versionNameSuffix = "-benchmark"
            applicationIdSuffix = ".benchmark"

            matchingFallbacks.addAll(commonMatchingFallbacks)
        }

        configureEach {
            // Replaces what initWith(release) copied.
            ndk.abiFilters.clear()
            ndk.abiFilters += if (name in releasedBuildTypes) releasedAbis else releasedAbis + emulatorAbis
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
            include(*(releasedAbis + emulatorAbis).toTypedArray())
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

// The libmpv AAR's components ship in the mpv add-on, outside any POM, so their entries come from its own inventory.
val libmpvLicencesAar = configurations.register("libmpvLicencesAar") {
    isCanBeConsumed = false
    isTransitive = false
}

val generateLibmpvLicences = tasks.register<GenerateLibmpvLicencesTask>("generateLibmpvLicences") {
    aar.from(libmpvLicencesAar)
    idGroup.set(libs.libmpv.android.map { it.group })
    configDir.set(layout.buildDirectory.dir("generated/libmpvLicences"))
}

val generateExtensionFamilies = tasks.register<GenerateExtensionFamiliesTask>("generateExtensionFamilies") {
    source.set(layout.projectDirectory.file("extension-compatibility.json"))
    packageName.set("eu.kanade.tachiyomi.extension.kind")
    outputDir.set(layout.buildDirectory.dir("generated/extensionFamilies"))
}

// Libraries resolved without a POM: the generated libmpv entries, and quickjs-kt with what it builds in (licences/)
val aboutLibrariesDefinitions = tasks.register<Sync>("aboutLibrariesDefinitions") {
    val quickjsKtVersion = libs.versions.quickjs.kt.get()
    from(generateLibmpvLicences.flatMap { it.configDir })
    from(layout.projectDirectory.dir("licences")) {
        filter { it.replace("@quickjsKtVersion@", quickjsKtVersion) }
    }
    into(layout.buildDirectory.dir("generated/aboutLibrariesDefinitions"))
}

aboutLibraries {
    collect {
        configPath.set(layout.dir(aboutLibrariesDefinitions.map { it.destinationDir }))
    }
}

dependencies {
    constraints {
        implementation(libs.androidx.concurrent.futures)
        implementation(libs.androidx.concurrent.futures.ktx)
    }

    baselineProfile(dependencies.project(mapOf("path" to projects.baselineProfile.path)))

    implementation(dependencies.project(mapOf("path" to projects.aniyomiSourceApi.path)))
    implementation(dependencies.project(mapOf("path" to projects.i18n.path)))
    implementation(dependencies.project(mapOf("path" to projects.core.archive.path)))
    implementation(dependencies.project(mapOf("path" to projects.core.common.path)))
    implementation(dependencies.project(mapOf("path" to projects.core.metro.path)))
    implementation(dependencies.project(mapOf("path" to projects.coreMetadata.path)))
    implementation(dependencies.project(mapOf("path" to projects.entryInteractions.path)))
    implementation(dependencies.project(mapOf("path" to projects.entrySourceApi.path)))
    implementation(dependencies.project(mapOf("path" to projects.sourceApi.path)))
    implementation(dependencies.project(mapOf("path" to projects.sourceCompat.path)))
    implementation(dependencies.project(mapOf("path" to projects.scriptRuntime.path)))
    implementation(dependencies.project(mapOf("path" to projects.sourceLocal.path)))
    implementation(dependencies.project(mapOf("path" to projects.data.path)))
    implementation(dependencies.project(mapOf("path" to projects.domain.path)))
    implementation(dependencies.project(mapOf("path" to projects.icons.customMaterialSymbols.path)))
    implementation(dependencies.project(mapOf("path" to projects.icons.materialSymbols.path)))
    implementation(dependencies.project(mapOf("path" to projects.icons.simpleIcons.path)))
    implementation(dependencies.project(mapOf("path" to projects.entryViewerChromeUi.path)))
    implementation(dependencies.project(mapOf("path" to projects.presentationCore.path)))
    implementation(dependencies.project(mapOf("path" to projects.presentationWidget.path)))
    implementation(dependencies.project(mapOf("path" to projects.telemetry.path)))
    implementation(dependencies.project(mapOf("path" to projects.modelArtifacts.runtime.path)))
    implementation(dependencies.project(mapOf("path" to projects.modelArtifacts.ui.path)))
    implementation(dependencies.project(mapOf("path" to projects.textRecognition.runtime.path)))
    implementation(dependencies.project(mapOf("path" to projects.textRecognition.providers.onnx.path)))
    // ML Kit needs Google Play services, so FOSS builds only list it.
    "fossImplementation"(dependencies.project(mapOf("path" to projects.textRecognition.providers.mlkit.catalog.path)))
    listOf("debug", "release", "preview", "benchmark").forEach { buildType ->
        "${buildType}Implementation"(
            dependencies.project(mapOf("path" to projects.textRecognition.providers.mlkit.path)),
        )
    }
    implementation(dependencies.project(mapOf("path" to projects.textRecognition.ui.path)))
    implementation(dependencies.project(mapOf("path" to projects.translation.runtime.path)))
    implementation(dependencies.project(mapOf("path" to projects.translation.ui.path)))
    implementation(dependencies.project(mapOf("path" to projects.translation.providers.deepl.path)))
    implementation(dependencies.project(mapOf("path" to projects.translation.providers.libretranslate.path)))
    implementation(dependencies.project(mapOf("path" to projects.tts.runtime.path)))
    implementation(dependencies.project(mapOf("path" to projects.tts.providers.android.path)))
    implementation(dependencies.project(mapOf("path" to projects.tts.ui.path)))
    implementation(dependencies.project(mapOf("path" to projects.videoEngine.mpv.path)))

    // Compose
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.animationGraphics)
    debugImplementation(libs.androidx.compose.uiTooling)
    implementation(libs.androidx.compose.uiToolingPreview)
    implementation(libs.androidx.compose.uiUtil)
    implementation(libs.androidx.navigationEvent)

    implementation(libs.androidx.interpolator)

    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    implementation(libs.kotlin.reflect)

    implementation(libs.bundles.kotlinx.coroutines)

    implementation(libs.sqldelight.async)

    implementation(libs.kotlinx.datetime)

    // AndroidX libraries
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.appCompat)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.core)
    implementation(libs.androidx.coreSplashScreen)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayerHls)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.datasourceOkhttp)
    implementation(libs.androidx.viewPager)
    implementation(libs.androidx.profileInstaller)

    implementation(libs.bundles.androidx.lifecycle)
    implementation(libs.metrox.viewmodel)
    implementation(libs.metrox.viewmodel.compose)

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
    implementation(libs.photoView)
    implementation(libs.directionalViewPager) {
        exclude(group = "androidx.viewpager", module = "viewpager")
    }
    implementation(libs.composeRichEditor)
    implementation(libs.aboutLibraries.core)
    // Its GitHub release has no metadata naming the artifact's type.
    libmpvLicencesAar(libs.libmpv.android) {
        artifact { type = "aar" }
    }
    implementation(libs.bundles.voyager)
    implementation(libs.composeMaterialMotion)
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

    // GraphQL generation
    implementation(libs.apollo)
    implementation(libs.apollo.adapters)

    // Tests
    testImplementation(libs.bundles.test)
    testRuntimeOnly(libs.junit.platform.launcher)

    // For detecting memory leaks; see https://square.github.io/leakcanary/
    // debugImplementation(libs.leakCanary.android)
    implementation(libs.leakCanary.plumber)

    testImplementation(libs.kotlinx.coroutines.test)
}

apollo {
    val schemaBasePath = "src/main/graphql/mihon/graphql"
    service("anilist") {
        packageName.set("mihon.graphql.anilist")
        val srcDir = "$schemaBasePath/anilist"
        srcDir(file(srcDir))

        introspection {
            endpointUrl.set("https://graphql.anilist.co")
            schemaFile.set(file("$srcDir/anilist.graphqls"))
        }

        // ISO 3166-1 alpha-2 country code
        mapScalarToKotlinString("CountryCode")
    }

    service("kitsu") {
        packageName.set("mihon.graphql.kitsu")
        val srcDir = "$schemaBasePath/kitsu"
        srcDir(file(srcDir))

        introspection {
            // Kitsu doesn't like requests without a UA
            headers.put(
                "User-Agent",
                "Katari v${android.defaultConfig.versionName} (${android.defaultConfig.applicationId})",
            )
            endpointUrl.set("https://kitsu.app/api/graphql")
            schemaFile.set(file("$srcDir/kitsu.graphqls"))
        }

        // A date, expressed as an ISO8601 string
        mapScalarToKotlinString("Date")
        // An ISO 8601-encoded datetime
        mapScalar("ISO8601DateTime", "kotlin.time.Instant", "com.apollographql.adapter.core.KotlinInstantAdapter")
        // A loose key-value map in GraphQL
        mapScalar(
            "Map",
            "kotlin.collections.Map<String, Any?>",
            "mihon.graphql.kitsu.adapter.KitsuMapAdapter",
        )
    }

    service("shikimori") {
        packageName.set("mihon.graphql.shikimori")
        val srcDir = "$schemaBasePath/shikimori"
        srcDir(file(srcDir))

        introspection {
            endpointUrl.set("https://shikimori.io/api/graphql")
            schemaFile.set(file("$srcDir/shikimori.graphqls"))
        }

        // An ISO 8601-encoded date
        mapScalarToKotlinString("ISO8601Date")
    }

    service("suwayomi") {
        packageName.set("mihon.graphql.suwayomi")
        val srcDir = "$schemaBasePath/suwayomi"
        srcDir(file(srcDir))

        introspection {
            endpointUrl.set("http://localhost:4567/api/graphql")
            schemaFile.set(file("$srcDir/suwayomi.graphqls"))
        }
    }
}

val latestCommitCount = getLatestCommitCount()
val latestCommitSha = getLatestCommitSha()
val latestCommitTime = getLatestCommitTime()
val currentTime = getCurrentTime()

fun ApplicationVariant.buildConfigField(type: String, name: String, value: Provider<String>) {
    buildConfigFields?.put(name, value.map { BuildConfigField(type, it, null) })
}

androidComponents {
    onVariants { variant ->
        if (variant.buildType !in releasedBuildTypes) return@onVariants
        variant.outputs
            .filter { output ->
                output.filters.any {
                    it.filterType == FilterConfiguration.FilterType.ABI &&
                        it.identifier in emulatorAbis
                }
            }
            .forEach { it.enabled.set(false) }
    }

    onVariants { variant ->
        val isUnstableBuild = variant.buildType == "debug" || variant.buildType == "preview"
        val buildTime = if (isUnstableBuild) currentTime else latestCommitTime

        variant.buildConfigField("String", "COMMIT_COUNT", latestCommitCount.map { "\"$it\"" })
        variant.buildConfigField("String", "COMMIT_SHA", latestCommitSha.map { "\"$it\"" })
        variant.buildConfigField("String", "BUILD_TIME", buildTime.map { "\"$it\"" })

        if (isUnstableBuild) {
            variant.outputs.forEach { output ->
                val versionName = output.versionName.get()
                output.versionName.set(latestCommitCount.map { "$versionName-$it" })
            }
        }

        if (Config.includeTelemetry) {
            variant.sources.manifests.addStaticManifestFile(
                projectDir.resolve("src/telemetry/AndroidManifest.xml").absolutePath,
            )
        }

        variant.sources.kotlin?.addGeneratedSourceDirectory(
            generateExtensionFamilies,
            GenerateExtensionFamiliesTask::outputDir,
        )

        val variantName = variant.name.replaceFirstChar { it.uppercase() }
        val checkNativeLibraries = tasks.register<CheckApkNativeLibrariesTask>("check${variantName}NativeLibraries") {
            apkDirectory.set(variant.artifacts.get(SingleArtifact.APK))
            report.set(layout.buildDirectory.file("reports/native-libraries/${variant.name}.txt"))
        }
        tasks.matching { it.name == "assemble$variantName" || it.name == "install$variantName" }.configureEach {
            dependsOn(checkNativeLibraries)
        }

        val resSource = variant.sources.res ?: return@onVariants

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

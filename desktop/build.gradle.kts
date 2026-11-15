import app.katari.gradle.Config
import app.katari.gradle.appimage.registerDesktopAppImage
import app.katari.gradle.desktopicon.GenerateDesktopIconTask
import app.katari.gradle.desktopicon.GenerateDesktopSplashTask
import app.katari.gradle.extensions.extensionHostJvmArguments
import app.katari.gradle.getAppVersionCode
import app.katari.gradle.getAppVersionName
import app.katari.gradle.getLatestCommitCount
import app.katari.gradle.getLatestCommitSha
import app.katari.gradle.getLatestCommitTime
import app.katari.gradle.tasks.GenerateDesktopBuildInfoTask

plugins {
    alias(katari.plugins.desktop.application)
    alias(katari.plugins.spotless)
    alias(libs.plugins.metro)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.aboutLibraries.jvm)
}

// The desktop app's own version, released and tagged `desktop-v<version>` apart from the Android app's. The release
// workflow reads it from this line.
version = "1.0.1"

val generateDesktopBuildInfo = tasks.register<GenerateDesktopBuildInfoTask>("generateDesktopBuildInfo") {
    packageName.set("app.katari.desktop.application")
    versionName.set(version.toString())
    androidVersionName.set(getAppVersionName())
    androidVersionCode.set(getAppVersionCode())
    commitSha.set(getLatestCommitSha())
    commitCount.set(getLatestCommitCount())
    // The commit's time rather than the build's, so the generated file stays the same between builds of one commit
    buildTime.set(getLatestCommitTime())
    release.set(Config.desktopRelease)
    chromiumVersion.set(katari.versions.chromium.asProvider())
    chromiumArchiveUrl.set(
        katari.versions.chromium.asProvider().map { chromium ->
            "https://github.com/pa2x2/katari/releases/download/chromium-$chromium/chromium-$chromium-linux-x86_64.tar.zst"
        },
    )
    chromiumArchiveSha256.set(katari.versions.chromium.linux.x64.sha256)
    chromiumArchiveBytes.set(katari.versions.chromium.linux.x64.size.map { it.toLong() })
    chromiumInstalledBytes.set(katari.versions.chromium.linux.x64.installed.size.map { it.toLong() })
    outputDir.set(layout.buildDirectory.dir("generated/desktopBuildInfo"))
}

kotlin {
    sourceSets.main {
        kotlin.srcDir(generateDesktopBuildInfo.flatMap { it.outputDir })
    }
}

// The Android app's launcher icon of the same build: the development one for every build but the release
val (iconSourceSet, iconName) = if (Config.desktopRelease) "main" to "ic_launcher" else "debug" to "ic_launcher_dev"
val iconDrawables = layout.settingsDirectory.dir("app/src/$iconSourceSet/res/drawable")
val generateDesktopIcon = tasks.register<GenerateDesktopIconTask>("generateDesktopIcon") {
    background.set(iconDrawables.file("${iconName}_background.xml"))
    foreground.set(iconDrawables.file("${iconName}_foreground.xml"))
    icon.set(layout.buildDirectory.file("generated/desktopIcon/app-icon.svg"))
}

// The same icon as the image the AppImage shows from the moment it is started until the app's window opens
val generateDesktopSplash = tasks.register<GenerateDesktopSplashTask>("generateDesktopSplash") {
    background.set(iconDrawables.file("${iconName}_background.xml"))
    foreground.set(iconDrawables.file("${iconName}_foreground.xml"))
    size.set(256)
    splash.set(layout.buildDirectory.file("generated/desktopSplash/splash.png"))
}

// The licences screen reads the definitions AboutLibraries exports from the app's dependencies, as the Android plugin
// generates them for the Android app. The app draws its icon for its windows and notifications.
tasks.processResources {
    from(tasks.named("exportLibraryDefinitions"))
    from(generateDesktopIcon)
}

// Libraries resolved without a POM: libarchive, libmpv and what they build in, whose entries tools/libarchive and
// tools/libmpv write to licences/
aboutLibraries {
    collect {
        configPath.set(layout.projectDirectory.dir("licences"))
    }
}

dependencies {
    implementation(projects.appUi)
    implementation(projects.features.appUpdate)
    implementation(projects.features.backup)
    implementation(projects.core.presentationCore)
    implementation(projects.core.data)
    implementation(projects.core.domain)
    implementation(projects.features.entryInteractions)
    implementation(projects.features.entryInteractions.host)
    implementation(projects.sources.entrySourceApi)
    implementation(projects.features.entryViewerSettingsUi)
    implementation(projects.sources.extensionRuntime)
    implementation(projects.sources.extensionRuntime.jvmHost)
    implementation(projects.core.common)
    implementation(projects.core.image)
    implementation(projects.core.metro)
    implementation(projects.core.platform)
    implementation(projects.core.i18n)
    implementation(projects.features.libraryUpdate)
    implementation(projects.features.sync.client)
    implementation(projects.core.icons.materialSymbols)
    implementation(projects.sources.scriptRuntime)
    implementation(projects.sources.sourceCompat)
    implementation(projects.sources.sourceLocal)
    implementation(projects.sources.webPages)
    implementation(projects.features.language.runtime)
    implementation(projects.features.modelArtifacts.runtime)
    implementation(projects.features.textRecognition.runtime)
    implementation(projects.features.textRecognition.providers.onnx)
    implementation(projects.features.textRecognition.ui)
    implementation(projects.features.translation.api)
    implementation(projects.features.translation.runtime)
    implementation(projects.features.translation.providers.deepl)
    implementation(projects.features.translation.providers.libretranslate)
    implementation(projects.features.translation.providers.serverConnection)
    implementation(projects.features.tts.runtime)
    implementation(projects.features.tts.providers.speechDispatcher)
    implementation(projects.sources.webView.chromium)
    implementation(projects.sources.webView.jcef)

    implementation(libs.metrox.viewmodel.compose)
    // The window routes the mouse back button to the navigation events the shared screens handle back with.
    implementation(libs.compose.multiplatform.navigationEvent)
    implementation(libs.coil.compose)
    implementation(libs.coil.core)
    implementation(libs.coil.network.okhttp)
    implementation(libs.kotlinx.coroutines.core)
    // Dispatchers.Main is the AWT event thread.
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.protobuf)
    // Extensions resolve the app's services through Injekt, as on Android
    implementation(libs.injekt)
    // The window manager calls AWT has none for, such as handing a later launch's startup id to the window
    implementation(libs.jna)
    implementation(libs.xmlutil.core)
    implementation(libs.xmlutil.serialization)
    // The native libraries the app's modules call, built for the system the app is packaged for, x86_64 Linux:
    // libarchive with its compression libraries built in, so archives open whatever the system has, as Android does
    // with its own build, and libmpv, built from the same sources as the Android add-on's
    runtimeOnly(libs.libarchive.linux.x64)
    runtimeOnly(libs.libmpv.linux.x64)

    testImplementation(libs.bundles.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}

// Libraries that come with parts of the app's libraries it never reaches
configurations.implementation {
    // Batik, with which moko-resources draws image resources kept as SVG; the app keeps only its strings there
    exclude(group = "org.apache.xmlgraphics")
    // Material 2, for the rich text editor's Material 2 fields; the app shows its Material 3 ones
    exclude(group = "org.jetbrains.compose.material", module = "material")
}

compose.desktop.application {
    mainClass = "app.katari.desktop.MainKt"
    jvmArgs += extensionHostJvmArguments
}

registerDesktopAppImage(
    release = Config.desktopRelease,
    icon = generateDesktopIcon.flatMap { it.icon },
    splash = generateDesktopSplash.flatMap { it.splash },
)

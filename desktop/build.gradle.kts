import mihon.gradle.Config
import mihon.gradle.appimage.registerDesktopAppImage
import mihon.gradle.desktopicon.GenerateDesktopIconTask
import mihon.gradle.extensions.extensionHostJvmArguments
import mihon.gradle.getAppVersionCode
import mihon.gradle.getAppVersionName
import mihon.gradle.getLatestCommitCount
import mihon.gradle.getLatestCommitSha
import mihon.gradle.getLatestCommitTime
import mihon.gradle.tasks.GenerateDesktopBuildInfoTask

plugins {
    alias(mihonx.plugins.desktop.application)
    alias(mihonx.plugins.spotless)
    alias(libs.plugins.metro)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.aboutLibraries.jvm)
}

// The desktop app's own version, released and tagged `desktop-v<version>` apart from the Android app's. The release
// workflow reads it from this line.
version = "0.1.0-alpha.4"

val generateDesktopBuildInfo = tasks.register<GenerateDesktopBuildInfoTask>("generateDesktopBuildInfo") {
    packageName.set("mihon.desktop.application")
    versionName.set(version.toString())
    androidVersionName.set(getAppVersionName())
    androidVersionCode.set(getAppVersionCode())
    commitSha.set(getLatestCommitSha())
    commitCount.set(getLatestCommitCount())
    // The commit's time rather than the build's, so the generated file stays the same between builds of one commit
    buildTime.set(getLatestCommitTime())
    release.set(Config.desktopRelease)
    outputDir.set(layout.buildDirectory.dir("generated/desktopBuildInfo"))
}

kotlin {
    sourceSets.main {
        kotlin.srcDir(generateDesktopBuildInfo.flatMap { it.outputDir })
    }
}

// The Android app's launcher icon of the same build: the development one for every build but the release
val generateDesktopIcon = tasks.register<GenerateDesktopIconTask>("generateDesktopIcon") {
    val (sourceSet, name) = if (Config.desktopRelease) "main" to "ic_launcher" else "debug" to "ic_launcher_dev"
    val drawables = layout.settingsDirectory.dir("app/src/$sourceSet/res/drawable")
    background.set(drawables.file("${name}_background.xml"))
    foreground.set(drawables.file("${name}_foreground.xml"))
    icon.set(layout.buildDirectory.file("generated/desktopIcon/app-icon.svg"))
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
    implementation(projects.appUpdate)
    implementation(projects.backup)
    implementation(projects.presentationCore)
    implementation(projects.data)
    implementation(projects.domain)
    implementation(projects.entryInteractions)
    implementation(projects.entryInteractions.host)
    implementation(projects.entrySourceApi)
    implementation(projects.entryViewerSettingsUi)
    implementation(projects.extensionRuntime)
    implementation(projects.extensionRuntime.jvmHost)
    implementation(projects.core.common)
    implementation(projects.core.image)
    implementation(projects.core.metro)
    implementation(projects.core.platform)
    implementation(projects.i18n)
    implementation(projects.libraryUpdate)
    implementation(projects.icons.materialSymbols)
    implementation(projects.scriptRuntime)
    implementation(projects.sourceCompat)
    implementation(projects.sourceLocal)
    implementation(projects.trackers)
    implementation(projects.language.runtime)
    implementation(projects.modelArtifacts.runtime)
    implementation(projects.textRecognition.runtime)
    implementation(projects.translation.api)
    implementation(projects.translation.runtime)
    implementation(projects.translation.providers.deepl)
    implementation(projects.translation.providers.libretranslate)
    implementation(projects.translation.providers.serverConnection)
    implementation(projects.tts.runtime)
    implementation(projects.webView.jcef)

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

    testImplementation(libs.bundles.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}

compose.desktop.application {
    mainClass = "mihon.desktop.MainKt"
    jvmArgs += extensionHostJvmArguments
}

registerDesktopAppImage(release = Config.desktopRelease, icon = generateDesktopIcon.flatMap { it.icon })

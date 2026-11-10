package app.katari.gradle.appimage

import app.katari.gradle.jetbrainsruntime.JCEF_NATIVE_ACCESS
import app.katari.gradle.jetbrainsruntime.jetBrainsRuntime
import app.katari.gradle.jetbrainsruntime.jetBrainsRuntimeSdk
import app.katari.gradle.jetbrainsruntime.provisionJetBrainsRuntime
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.file.RegularFile
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.desktop.DesktopExtension

/**
 * Registers `packageAppImage`, which packs the desktop app with the JetBrains Runtime into
 * `build/appimage/<name>-<version>-x86_64.AppImage`, as [release] or as the development build. The two install side by
 * side under their own names, as `DesktopAppVariant` keeps their data apart; only the release opens the app's links.
 * [icon] is the SVG of that build's icon, and [splash] the PNG the AppImage shows while the app starts.
 */
fun Project.registerDesktopAppImage(release: Boolean, icon: Provider<RegularFile>, splash: Provider<RegularFile>) {
    if (!isAppImageArchitecture()) {
        tasks.register("packageAppImage") {
            group = "distribution"
            doLast { throw GradleException("The desktop app's AppImage is built for x86_64 only") }
        }
        return
    }
    val identity = if (release) {
        AppImageIdentity(
            "app.katari.desktop",
            "Katari",
            "Katari",
            "katari",
            listOf("katari", "tachiyomi", "app.katari"),
        )
    } else {
        AppImageIdentity("app.katari.desktop.dev", "Katari Dev", "Katari-Dev", "katari-dev", emptyList())
    }
    val application = (extensions.getByType<ComposeExtension>() as ExtensionAware).extensions
        .getByType<DesktopExtension>().application
    val runtime = jetBrainsRuntime()
    val provisionRuntime = provisionJetBrainsRuntime(runtime)
    val sdk = jetBrainsRuntimeSdk()
    val provisionSdk = provisionJetBrainsRuntime(sdk)
    val tools = appImageTools()
    val provisionTools = provisionAppImageTools(tools)
    val jar = tasks.named<Jar>("jar")
    val libraries = appImageClasspath().incoming.artifacts.resolvedArtifacts
        .map { artifacts -> artifacts.distinctBy { it.file } }

    val linkRuntime = tasks.register<LinkAppImageRuntimeTask>("linkAppImageRuntime") {
        description = "Links the runtime the desktop app's AppImage ships from the JetBrains Runtime's SDK."
        dependsOn(provisionRuntime, provisionSdk)
        sdkName.set(sdk.archiveName)
        sdkHome.set(sdk.home)
        runtimeRelease.set(runtime.release)
        excludedModules.set(LinkAppImageRuntimeTask.DEVELOPMENT_MODULES)
        this.runtime.set(layout.buildDirectory.dir("appimage/runtime"))
    }
    val checkModules = tasks.register<CheckAppImageModulesTask>("checkAppImageModules") {
        description = "Checks that the AppImage's runtime has every module the desktop app's jars need."
        dependsOn(provisionSdk)
        jars.from(jar.flatMap { it.archiveFile })
        jars.from(libraries.map { artifacts -> artifacts.map { it.file } })
        sdkName.set(sdk.archiveName)
        sdkHome.set(sdk.home)
        runtimeRelease.set(linkRuntime.flatMap { it.runtime.file("release") })
        report.set(layout.buildDirectory.file("reports/appimage/modules.txt"))
    }

    val assembleAppDir = tasks.register<AssembleAppDirTask>("assembleAppDir") {
        description = "Lays out the desktop app with its runtime as the folder its AppImage is packed from."
        dependsOn(checkModules)
        jars.from(jar.flatMap { it.archiveFile })
        jars.from(libraries.map { artifacts -> artifacts.map { it.file } })
        jarNames.set(
            jar.flatMap { it.archiveFileName }.zip(libraries) { appJar, artifacts ->
                (listOf(appJar) + artifacts.map { it.libName() }).also(::checkDistinct)
            },
        )
        runtimeHome.set(linkRuntime.flatMap { it.runtime })
        mainClass.set(provider { application.mainClass })
        jvmArguments.set(provider { (application.jvmArgs + JCEF_NATIVE_ACCESS).distinct() })
        applicationId.set(identity.applicationId)
        displayName.set(identity.displayName)
        directoryName.set(identity.directoryName)
        linkSchemes.set(identity.linkSchemes)
        this.icon.set(icon)
        this.splash.set(splash)
        appDir.set(layout.buildDirectory.dir("appimage/${identity.fileName}.AppDir"))
    }
    tasks.register<PackAppImageTask>("packageAppImage") {
        group = "distribution"
        description = "Packs the desktop app into an AppImage; -Pdesktop-release packs the release."
        dependsOn(provisionTools)
        appDir.set(assembleAppDir.flatMap { it.appDir })
        toolPins.set(listOf(tools.appimagetool.sha256, tools.runtime.sha256))
        appimagetool.set(tools.appimagetool.file)
        this.runtime.set(tools.runtime.file)
        appImage.set(
            layout.buildDirectory.file("appimage/${identity.fileName}-$version-$ARCHITECTURE.AppImage"),
        )
    }
}

/**
 * The app's runtime class path as the AppImage ships it: with Skiko's natives for Linux x86_64 only, where Compose
 * brings those of every platform it runs on, which took a ninth of the AppImage. Skiko loads the natives for the
 * platform it runs on, and the AppImage runs on that one.
 */
private fun Project.appImageClasspath(): Configuration {
    val runtimeClasspath = configurations.getByName("runtimeClasspath")
    return configurations.create("appImageRuntimeClasspath") {
        isCanBeConsumed = false
        isCanBeResolved = true
        setExtendsFrom(runtimeClasspath.extendsFrom)
        // The same variants of each library and module as the app's own run resolves
        runtimeClasspath.attributes.keySet().forEach { attributes.copy(it, runtimeClasspath.attributes) }
        resolutionStrategy.eachDependency {
            if (requested.group == SKIKO_GROUP && requested.name == SKIKO_ALL_PLATFORMS) {
                useTarget("$SKIKO_GROUP:$SKIKO_LINUX_X64:${requested.version}")
                because("the AppImage runs on Linux x86_64 only")
            }
        }
    }
}

private fun <T : Any> AttributeContainer.copy(key: Attribute<T>, from: AttributeContainer) {
    from.getAttribute(key)?.let { attribute(key, it) }
}

private const val SKIKO_GROUP = "org.jetbrains.skiko"
private const val SKIKO_ALL_PLATFORMS = "skiko-awt-runtime-all"
private const val SKIKO_LINUX_X64 = "skiko-awt-runtime-linux-x64"

private class AppImageIdentity(
    val applicationId: String,
    val displayName: String,
    /** How the AppImage's file name starts. */
    val fileName: String,
    /** The name of the app's own folder in each XDG base directory, as `DesktopAppVariant` names it. */
    val directoryName: String,
    val linkSchemes: List<String>,
)

/** A name in the AppDir's `lib/` that no other jar has, as many modules' jars share names such as `api.jar`. */
private fun ResolvedArtifactResult.libName(): String = when (val component = id.componentIdentifier) {
    is ProjectComponentIdentifier -> "${component.projectPath.removePrefix(":").replace(':', '-')}-${file.name}"
    is ModuleComponentIdentifier -> "${component.group}-${file.name}"
    else -> file.name
}

private fun checkDistinct(names: List<String>) {
    val repeated = names.groupBy { it }.filterValues { it.size > 1 }.keys
    if (repeated.isNotEmpty()) throw GradleException("Jars of the desktop app share names in its AppImage: $repeated")
}

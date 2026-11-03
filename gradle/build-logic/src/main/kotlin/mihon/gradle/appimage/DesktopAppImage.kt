package mihon.gradle.appimage

import mihon.gradle.jetbrainsruntime.JCEF_NATIVE_ACCESS
import mihon.gradle.jetbrainsruntime.jetBrainsRuntime
import mihon.gradle.jetbrainsruntime.provisionJetBrainsRuntime
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.desktop.DesktopExtension
import java.io.File

/**
 * Registers `packageAppImage`, which packs the desktop app with the JetBrains Runtime into
 * `build/appimage/<name>-<version>-x86_64.AppImage`, as [release] or as the development build. The two install side by
 * side under their own names, as `DesktopAppVariant` keeps their data apart; only the release opens the app's links.
 */
fun Project.registerDesktopAppImage(release: Boolean) {
    if (!isAppImageArchitecture()) {
        tasks.register("packageAppImage") {
            group = "distribution"
            doLast { throw GradleException("The desktop app's AppImage is built for x86_64 only") }
        }
        return
    }
    val identity = if (release) {
        AppImageIdentity("app.katari.desktop", "Katari", "Katari", listOf("katari", "tachiyomi", "app.katari"))
    } else {
        AppImageIdentity("app.katari.desktop.dev", "Katari Dev", "Katari-Dev", emptyList())
    }
    val application = (extensions.getByType<ComposeExtension>() as ExtensionAware).extensions
        .getByType<DesktopExtension>().application
    val runtime = jetBrainsRuntime()
    val provisionRuntime = provisionJetBrainsRuntime(runtime)
    val tools = appImageTools()
    val provisionTools = provisionAppImageTools(tools)
    val jar = tasks.named<Jar>("jar")
    val libraries = configurations.named("runtimeClasspath").flatMap { it.incoming.artifacts.resolvedArtifacts }
        .map { artifacts -> artifacts.distinctBy { it.file } }

    val assembleAppDir = tasks.register<AssembleAppDirTask>("assembleAppDir") {
        description = "Lays out the desktop app with the JetBrains Runtime as the folder its AppImage is packed from."
        dependsOn(provisionRuntime)
        jars.from(jar.flatMap { it.archiveFile })
        jars.from(libraries.map { artifacts -> artifacts.map { it.file } })
        jarNames.set(
            jar.flatMap { it.archiveFileName }.zip(libraries) { appJar, artifacts ->
                (listOf(appJar) + artifacts.map { it.libName() }).also(::checkDistinct)
            },
        )
        runtimeName.set(runtime.archiveName)
        runtimeHome.set(File(runtime.home.absolutePath))
        mainClass.set(provider { application.mainClass })
        jvmArguments.set(provider { (application.jvmArgs + JCEF_NATIVE_ACCESS).distinct() })
        applicationId.set(identity.applicationId)
        displayName.set(identity.displayName)
        linkSchemes.set(identity.linkSchemes)
        // The logo the website shows, kept once
        icon.set(layout.settingsDirectory.file("website/src/site/katari.svg"))
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

private class AppImageIdentity(
    val applicationId: String,
    val displayName: String,
    /** How the AppImage's file name starts. */
    val fileName: String,
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

package mihon.gradle.appimage

import mihon.gradle.extensions.mihonx
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register
import java.io.File
import java.io.Serializable

/**
 * The tools that pack the desktop app into an AppImage, pinned in `gradle/mihon.versions.toml`: appimagetool, and the
 * AppImage runtime it puts in front of the packed files, handed to it so it doesn't fetch the newest one. Both are
 * downloaded once per machine into the Gradle user home, where every build and worktree finds them.
 */
internal class AppImageTools(
    val appimagetool: PinnedTool,
    val runtime: PinnedTool,
)

/** A single file the build downloads by its pin to [file]. */
data class PinnedTool(val name: String, val url: String, val sha256: String, val file: File) : Serializable

/** Whether this machine builds AppImages: the pinned tools run on x86_64 and pack for it. */
internal fun isAppImageArchitecture(): Boolean = System.getProperty("os.arch") in setOf("amd64", "x86_64")

internal fun Project.appImageTools(): AppImageTools {
    val cache = gradle.gradleUserHomeDir.resolve("caches/katari/appimage")
    val appimagetool = mihonx.versions.appimagetool
    val runtime = mihonx.versions.appimage.runtime
    return AppImageTools(
        appimagetool = PinnedTool(
            name = "appimagetool",
            url = "https://github.com/AppImage/appimagetool/releases/download/${appimagetool.asProvider().get()}/" +
                "appimagetool-$ARCHITECTURE.AppImage",
            sha256 = appimagetool.sha256.get(),
            file = cache.resolve("appimagetool-${appimagetool.asProvider().get()}-$ARCHITECTURE.AppImage"),
        ),
        runtime = PinnedTool(
            name = "the AppImage runtime",
            url = "https://github.com/AppImage/type2-runtime/releases/download/${runtime.asProvider().get()}/" +
                "runtime-$ARCHITECTURE",
            sha256 = runtime.sha256.get(),
            file = cache.resolve("runtime-${runtime.asProvider().get()}-$ARCHITECTURE"),
        ),
    )
}

/** This project's task that makes [tools] ready. */
internal fun Project.provisionAppImageTools(tools: AppImageTools): TaskProvider<ProvisionAppImageToolsTask> {
    val offline = gradle.startParameter.isOffline
    return tasks.register<ProvisionAppImageToolsTask>("provisionAppImageTools") {
        description = "Downloads and verifies the tools that pack the desktop app into an AppImage."
        this.tools.set(listOf(tools.appimagetool, tools.runtime))
        this.offline.set(offline)
    }
}

/** The architecture of the AppImage, as AppImage's tools and file names spell it. */
internal const val ARCHITECTURE = "x86_64"

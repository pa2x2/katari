package mihon.tools.extensioncompatibility.environment

import app.katari.desktop.extension.ExtensionHostGraph
import app.katari.desktop.extension.startHeadlessExtensionHost
import app.katari.desktop.storage.XdgDesktopDirectories
import logcat.LogPriority
import logcat.LogcatLogger
import java.nio.file.Files
import java.nio.file.Path

/**
 * The desktop extension host of one run, on a temporary folder standing for the user's home, so the run's database,
 * settings, apks and converted jars never touch the user's desktop data. The folder is deleted when the run closes
 * unless it's kept for inspection; the host's log is in it.
 */
class SmokeEnvironment private constructor(
    val root: Path,
    val host: ExtensionHostGraph,
    private val keepData: Boolean,
) : AutoCloseable {

    override fun close() {
        if (!keepData) root.toFile().deleteRecursively()
    }

    companion object {
        suspend fun start(keepData: Boolean): SmokeEnvironment {
            val root = Files.createTempDirectory("katari-smoke-")
            LogcatLogger.install()
            LogcatLogger.loggers += RunLogger(root.resolve("host.log"), LogPriority.INFO)
            val directories = XdgDesktopDirectories(
                "katari",
                mapOf(
                    "HOME" to root.toString(),
                    "XDG_DATA_HOME" to root.resolve("data").toString(),
                    "XDG_CONFIG_HOME" to root.resolve("config").toString(),
                    "XDG_CACHE_HOME" to root.resolve("cache").toString(),
                ),
            )
            return SmokeEnvironment(root, startHeadlessExtensionHost(directories), keepData)
        }
    }
}

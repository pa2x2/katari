package mihon.desktop.storage

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path

/** Linux, laid out by the XDG Base Directory specification. */
class XdgDesktopDirectories(
    private val environment: Map<String, String> = System.getenv(),
) : DesktopDirectories {

    override val data: Path by lazy { appDirectory("XDG_DATA_HOME", ".local/share") }

    override val configuration: Path by lazy { appDirectory("XDG_CONFIG_HOME", ".config") }

    override val cache: Path by lazy { appDirectory("XDG_CACHE_HOME", ".cache") }

    override val home: Path by lazy {
        Path(environment["HOME"]?.takeIf { it.isNotEmpty() } ?: System.getProperty("user.home"))
    }

    private fun appDirectory(variable: String, homeRelativeDefault: String): Path {
        return Files.createDirectories(baseDirectory(variable, homeRelativeDefault).resolve(APP_DIRECTORY_NAME))
    }

    private fun baseDirectory(variable: String, homeRelativeDefault: String): Path {
        // The specification counts an empty or relative value as invalid, to be ignored like an unset variable.
        environment[variable]?.let(::Path)?.takeIf { it.isAbsolute }?.let { return it }
        return home.resolve(homeRelativeDefault)
    }

    private companion object {
        const val APP_DIRECTORY_NAME = "katari"
    }
}

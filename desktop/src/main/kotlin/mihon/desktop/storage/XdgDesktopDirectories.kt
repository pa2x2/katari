package mihon.desktop.storage

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.readLines

/**
 * Linux, laid out by the XDG Base Directory specification, with the user's own folders, such as pictures, from
 * `user-dirs.dirs`, which xdg-user-dirs writes in the language of the user's desktop.
 */
class XdgDesktopDirectories(
    private val environment: Map<String, String> = System.getenv(),
) : DesktopDirectories {

    override val data: Path by lazy { appDirectory("XDG_DATA_HOME", ".local/share") }

    override val configuration: Path by lazy { appDirectory("XDG_CONFIG_HOME", ".config") }

    override val cache: Path by lazy { appDirectory("XDG_CACHE_HOME", ".cache") }

    override val state: Path by lazy { appDirectory("XDG_STATE_HOME", ".local/state") }

    // The specification has no default for the runtime directory; the session manager sets it or leaves it unset.
    override val runtime: Path by lazy {
        environment["XDG_RUNTIME_DIR"]?.let(::Path)?.takeIf { it.isAbsolute }
            ?.let { Files.createDirectories(it.resolve(APP_DIRECTORY_NAME)) }
            ?: state
    }

    override val home: Path by lazy {
        Path(environment["HOME"]?.takeIf { it.isNotEmpty() } ?: System.getProperty("user.home"))
    }

    override val pictures: Path? by lazy { userDirectory("XDG_PICTURES_DIR") }

    /**
     * The folder [variable] names in `user-dirs.dirs`, or else in the environment, as `xdg-user-dir` reads it. The file
     * writes a folder in the home directory as `"$HOME/name"` and any other as an absolute path; one that is the home
     * directory itself means the user has none.
     */
    private fun userDirectory(variable: String): Path? {
        val configured = userDirsFile()?.let { file ->
            file.readLines().firstNotNullOfOrNull { line ->
                USER_DIR_LINE.matchEntire(line.trim())?.takeIf { it.groupValues[1] == variable }?.groupValues?.get(2)
            }
        } ?: environment[variable] ?: return null
        val value = configured.replace(ESCAPED_CHARACTER, "$1")
        val path = if (value == HOME_VARIABLE || value.startsWith("$HOME_VARIABLE/")) {
            Path(home.toString() + value.removePrefix(HOME_VARIABLE))
        } else {
            Path(value).takeIf { it.isAbsolute } ?: return null
        }
        return path.normalize().takeUnless { it == home.normalize() }
    }

    private fun userDirsFile(): Path? =
        baseDirectory("XDG_CONFIG_HOME", ".config").resolve("user-dirs.dirs").takeIf(Files::isRegularFile)

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
        const val HOME_VARIABLE = "\$HOME"

        // `XDG_PICTURES_DIR="$HOME/Pictures"`, a shell assignment with the value in double quotes
        val USER_DIR_LINE = Regex("""^(XDG_[A-Z]+_DIR)="(.*)"$""")
        val ESCAPED_CHARACTER = Regex("""\\(.)""")
    }
}

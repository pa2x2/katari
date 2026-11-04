package mihon.video.engine.mpv.player

import android.content.Context
import java.io.File

/**
 * mpv's configuration directory, which holds only the `fonts.conf` libass needs to find Android's fonts. The add-on's
 * fontconfig has no configuration of its own and finds no fonts without it, so ASS subtitles, which mpv draws, would
 * show nothing. mpv reads the file from its configuration directory only, so that directory is used, with no
 * `mpv.conf` in it. Fontconfig keeps its scan of the font directories in the app's cache.
 */
internal fun mpvConfigDirectory(context: Context): File {
    val directory = File(context.noBackupFilesDir, CONFIG_DIRECTORY)
    val fontsConf = File(directory, "fonts.conf")
    val content = fontsConf(File(context.cacheDir, FONT_CACHE_DIRECTORY))
    if (!fontsConf.isFile || fontsConf.readText() != content) {
        directory.mkdirs()
        fontsConf.writeText(content)
    }
    return directory
}

// Android names no default families in an API, so these are the ones it ships, as mpv-android sets them.
private fun fontsConf(cacheDirectory: File): String = """
    <fontconfig>
    <dir>/system/fonts/</dir>
    <dir>/product/fonts/</dir>
    <cachedir>${cacheDirectory.path}</cachedir>
    <alias><family>serif</family><prefer><family>Noto Serif</family></prefer></alias>
    <alias><family>sans-serif</family><prefer><family>Roboto</family><family>Noto Sans</family></prefer></alias>
    <alias><family>monospace</family><prefer><family>Droid Sans Mono</family></prefer></alias>
    </fontconfig>
""".trimIndent()

private const val CONFIG_DIRECTORY = "mpv"
private const val FONT_CACHE_DIRECTORY = "mpv-fontconfig"

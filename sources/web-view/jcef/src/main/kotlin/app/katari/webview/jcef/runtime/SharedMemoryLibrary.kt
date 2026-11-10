package app.katari.webview.jcef.runtime

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.exists

/**
 * Puts [library], the Java runtime's library for the memory Chromium's server hands the pages' pictures over in,
 * beside the server in [chromium]. The library belongs to the runtime, whose JCEF classes it is the native half of,
 * but JCEF loads it from beside the server whenever the server is outside the runtime; without it the first picture
 * of a page fails and takes the connection to the server with it. Copied again when the runtime's differs, as after
 * an update of the app. Throws an [IOException] when it can't be put there.
 */
internal fun placeBesideChromium(library: Path, chromium: Path) {
    val placed = chromium.resolve(library.fileName)
    if (placed.exists() && Files.mismatch(library, placed) == SAME) return
    Files.copy(library, placed, StandardCopyOption.REPLACE_EXISTING)
}

private const val SAME = -1L

package app.katari.tools.extensioncompatibility.options

import java.nio.file.Path
import kotlin.io.path.readLines

/** The store index URLs in [file], one per line; blank lines and lines starting with `#` are left out. */
fun readStoreList(file: Path): List<String> = file.readLines()
    .map(String::trim)
    .filter { it.isNotEmpty() && !it.startsWith('#') }
    .distinct()

package app.katari.sync.server.web.page

import java.util.Locale

/** Decimal units, as phones and desktop file managers show sizes. */
internal fun byteSizeText(bytes: Long): String {
    if (bytes < 1000) return "$bytes B"
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1000 && unit < UNITS.lastIndex) {
        value /= 1000
        unit++
    }
    val number = if (value < 10) String.format(Locale.ROOT, "%.1f", value) else value.toLong().toString()
    return "$number ${UNITS[unit]}"
}

private val UNITS = listOf("B", "kB", "MB", "GB", "TB")

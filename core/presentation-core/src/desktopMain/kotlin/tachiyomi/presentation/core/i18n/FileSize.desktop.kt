package tachiyomi.presentation.core.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.util.Locale

@Composable
actual fun fileSize(bytes: Long): String = remember(bytes) { formatSize(bytes, shorter = false) }

@Composable
actual fun shortFileSize(bytes: Long): String = remember(bytes) { formatSize(bytes, shorter = true) }

/**
 * Rounds as Android's `Formatter` does, so a size reads the same on both platforms: SI units, a unit step once the
 * value passes 900, and fewer decimals the larger the value, fewer still when [shorter].
 */
private fun formatSize(bytes: Long, shorter: Boolean): String {
    var value = bytes.toDouble()
    var unit = 0
    while (value > UNIT_STEP_THRESHOLD && unit < UNITS.lastIndex) {
        value /= SI_STEP
        unit++
    }
    val decimals = when {
        unit == 0 || value >= 100 -> 0
        value < 1 -> 2
        !shorter -> 2
        value < 10 -> 1
        else -> 0
    }
    return "%.${decimals}f %s".format(Locale.getDefault(), value, UNITS[unit])
}

private val UNITS = listOf("B", "kB", "MB", "GB", "TB", "PB")

private const val SI_STEP = 1000.0

private const val UNIT_STEP_THRESHOLD = 900.0

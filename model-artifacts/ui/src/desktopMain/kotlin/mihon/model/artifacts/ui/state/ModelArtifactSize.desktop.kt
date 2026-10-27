package mihon.model.artifacts.ui.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.util.Locale

/**
 * Rounds as Android's `Formatter.formatShortFileSize` does, so a model reads the same size on both platforms: SI units,
 * a unit step once the value passes 900, and fewer decimals the larger the value.
 */
@Composable
actual fun formatModelArtifactSize(bytes: Long): String = remember(bytes) { shortSize(bytes) }

private fun shortSize(bytes: Long): String {
    var value = bytes.toDouble()
    var unit = 0
    while (value > UNIT_STEP_THRESHOLD && unit < UNITS.lastIndex) {
        value /= SI_STEP
        unit++
    }
    val decimals = when {
        unit == 0 || value >= 10 -> 0
        value < 1 -> 2
        else -> 1
    }
    return "%.${decimals}f %s".format(Locale.getDefault(), value, UNITS[unit])
}

private val UNITS = listOf("B", "kB", "MB", "GB", "TB", "PB")

private const val SI_STEP = 1000.0

private const val UNIT_STEP_THRESHOLD = 900.0

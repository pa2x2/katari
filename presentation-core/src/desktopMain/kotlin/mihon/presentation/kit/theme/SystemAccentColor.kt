package mihon.presentation.kit.theme

import kotlinx.coroutines.flow.StateFlow

/** The accent colour the desktop publishes, which the Dynamic theme is seeded from. */
interface SystemAccentColor {
    /** The accent as opaque ARGB; null where the desktop publishes none or can't be asked. */
    val accentArgb: StateFlow<Long?>
}

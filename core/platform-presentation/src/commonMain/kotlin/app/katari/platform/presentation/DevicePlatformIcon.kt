package app.katari.platform.presentation

import androidx.compose.ui.graphics.vector.ImageVector
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Devices
import app.katari.icons.platformsymbols.PlatformSymbols
import app.katari.icons.platformsymbols.rounded.Computer
import app.katari.icons.platformsymbols.rounded.Smartphone

/**
 * Pictures a device by the platform it names itself with, such as another device of a sync account. The names are free
 * text, so unknown ones show a neutral icon.
 */
fun devicePlatformIcon(platform: String): ImageVector = when (platform.lowercase()) {
    "android" -> PlatformSymbols.Rounded.Smartphone
    "linux", "windows", "macos" -> PlatformSymbols.Rounded.Computer
    else -> MaterialSymbols.Rounded.Devices
}

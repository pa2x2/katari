package app.katari.platform.presentation

import app.katari.icons.platformsymbols.PlatformSymbols
import app.katari.icons.platformsymbols.rounded.Computer
import app.katari.icons.platformsymbols.rounded.ComputerCancel
import app.katari.icons.platformsymbols.rounded.Mouse

actual val platformPresentation = PlatformPresentation(
    thisDeviceIcon = PlatformSymbols.Rounded.Computer,
    thisDeviceRemovedIcon = PlatformSymbols.Rounded.ComputerCancel,
    pointerIcon = PlatformSymbols.Rounded.Mouse,
)

package app.katari.platform.presentation

import app.katari.icons.platformsymbols.PlatformSymbols
import app.katari.icons.platformsymbols.rounded.PhonelinkErase
import app.katari.icons.platformsymbols.rounded.Smartphone
import app.katari.icons.platformsymbols.rounded.TouchApp

actual val platformPresentation = PlatformPresentation(
    thisDeviceIcon = PlatformSymbols.Rounded.Smartphone,
    thisDeviceRemovedIcon = PlatformSymbols.Rounded.PhonelinkErase,
    pointerIcon = PlatformSymbols.Rounded.TouchApp,
)

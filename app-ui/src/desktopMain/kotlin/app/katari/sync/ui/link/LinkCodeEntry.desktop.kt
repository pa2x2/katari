package app.katari.sync.ui.link

import cafe.adriel.voyager.core.screen.Screen

internal actual val linkCodeScanning: Boolean = false

internal actual fun linkCodeEntryScreen(): Screen = EnterLinkCodeScreen()

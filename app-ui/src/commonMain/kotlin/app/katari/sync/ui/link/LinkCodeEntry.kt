package app.katari.sync.ui.link

import cafe.adriel.voyager.core.screen.Screen

/** Whether this platform reads a linked device's code with the camera; elsewhere it is typed (D051). */
internal expect val linkCodeScanning: Boolean

/** Where a new device takes a linked device's code: the camera's scanner, or the screen it is typed on. */
internal expect fun linkCodeEntryScreen(): Screen

package app.katari.presentation.kit.window

import androidx.compose.ui.unit.DpSize

/** Desktop windows keep the orientation of the screen they are on. */
internal actual fun honoursOrientationRequests(size: DpSize, inMultiWindow: Boolean): Boolean = false

package mihon.presentation.kit.window

import androidx.compose.ui.unit.DpSize

/** Whether the platform turns a window of [size] to an orientation the app asks for. */
internal expect fun honoursOrientationRequests(size: DpSize, inMultiWindow: Boolean): Boolean

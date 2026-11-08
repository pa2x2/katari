package app.katari.presentation.kit.gesture

import androidx.compose.ui.Modifier

// Desktop windows have no edge gestures to take drags from content
actual fun Modifier.excludeFromSystemGestures(): Modifier = this

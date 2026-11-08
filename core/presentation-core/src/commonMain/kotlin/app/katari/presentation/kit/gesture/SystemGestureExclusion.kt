package app.katari.presentation.kit.gesture

import androidx.compose.ui.Modifier

/** Keeps the platform's edge gestures, such as Android's back swipe, from taking drags that start on this element. */
expect fun Modifier.excludeFromSystemGestures(): Modifier

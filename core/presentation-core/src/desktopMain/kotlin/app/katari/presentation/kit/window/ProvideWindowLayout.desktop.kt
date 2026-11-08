package app.katari.presentation.kit.window

import androidx.compose.runtime.Composable

@Composable
internal actual fun rememberWindowFold(): WindowFold? = null

@Composable
internal actual fun rememberMultiWindowCheck(): () -> Boolean = { false }

// Desktop has no folds, so nothing places surfaces by the last touch
@Composable
internal actual fun rememberWindowLastTouch(): LastTouch? = null

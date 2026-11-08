package tachiyomi.presentation.core.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

// Android's defaults, so lists look and behave the same on both platforms

@Composable
internal actual fun scrollbarThickness(): Float = with(LocalDensity.current) { 4.dp.toPx() }

internal actual val ScrollbarFadeDelayMillis: Long = 300
internal actual val ScrollbarFadeDurationMillis: Int = 250

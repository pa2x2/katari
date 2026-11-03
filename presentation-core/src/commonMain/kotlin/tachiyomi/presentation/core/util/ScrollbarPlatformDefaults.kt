package tachiyomi.presentation.core.util

import androidx.compose.runtime.Composable

/** Scrollbar thickness in pixels, as the platform draws its own scrollbars. */
@Composable
internal expect fun scrollbarThickness(): Float

/** How long a scrollbar stays after scrolling stops, and how long it then takes to fade. */
internal expect val ScrollbarFadeDelayMillis: Long
internal expect val ScrollbarFadeDurationMillis: Int

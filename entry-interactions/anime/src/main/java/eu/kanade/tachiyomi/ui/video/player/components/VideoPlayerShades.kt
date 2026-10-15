package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import mihon.presentation.kit.theme.KatariTheme

/** The shade behind controls along the top edge of the video, fading out toward the middle. */
@Composable
@ReadOnlyComposable
internal fun videoPlayerTopShade(): Brush =
    Brush.verticalGradient(listOf(KatariTheme.colors.mediaScrim, Color.Transparent))

/** The shade behind controls along the bottom edge of the video, fading out toward the middle. */
@Composable
@ReadOnlyComposable
internal fun videoPlayerBottomShade(): Brush =
    Brush.verticalGradient(listOf(Color.Transparent, KatariTheme.colors.mediaScrim))

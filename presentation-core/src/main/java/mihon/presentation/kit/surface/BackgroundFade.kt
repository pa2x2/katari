package mihon.presentation.kit.surface

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import mihon.presentation.kit.theme.KatariTheme

/** Fades what lies under this element into the screen's background toward its bottom, such as text cut off mid-line. */
@Composable
fun Modifier.fadeIntoBackground(): Modifier =
    background(Brush.verticalGradient(listOf(Color.Transparent, KatariTheme.colors.background)))

/** A soft patch of the screen's background behind a small control, so it reads over the content it overlaps. */
@Composable
fun Modifier.backgroundHalo(): Modifier =
    background(Brush.radialGradient(listOf(KatariTheme.colors.background, Color.Transparent)))

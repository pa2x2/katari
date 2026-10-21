package mihon.presentation.kit.progress

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.LocalContentColor
import mihon.presentation.kit.theme.color
import androidx.compose.material3.LoadingIndicator as MaterialLoadingIndicator

/**
 * The app's only indeterminate wait indicator: the Material 3 Expressive morphing shapes that
 * Android's own UI uses. Determinate progress keeps its ring or bar so the amount stays readable.
 */
@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    tone: ContentTone = ContentTone.Primary,
) {
    MaterialLoadingIndicator(
        modifier = modifier,
        color = tone.color(fallback = LocalContentColor.current),
    )
}

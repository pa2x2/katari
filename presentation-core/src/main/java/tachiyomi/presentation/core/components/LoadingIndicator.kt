package tachiyomi.presentation.core.components

import androidx.compose.material3.LoadingIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.material3.LoadingIndicator as MaterialLoadingIndicator

/**
 * The app's only indeterminate wait indicator: the Material 3 Expressive morphing shapes that
 * Android's own UI uses. Determinate progress keeps its ring or bar so the amount stays readable.
 */
@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
) {
    MaterialLoadingIndicator(
        modifier = modifier,
        color = color.takeOrElse { LoadingIndicatorDefaults.indicatorColor },
    )
}

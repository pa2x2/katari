package tachiyomi.presentation.core.components.reader

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Minimal progress affordance shown at a chapter boundary while its destination loads. It occupies
 * the slot the destination content will occupy, so no further interaction is needed once loading
 * completes.
 */
@Composable
fun ReaderEntryChildTransitionLoadingIndicator(
    loadingDescription: String,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    indicatorSize: Dp = 32.dp,
) {
    CircularProgressIndicator(
        modifier = modifier
            .size(indicatorSize)
            .semantics { contentDescription = loadingDescription },
        color = accentColor,
        strokeWidth = 3.dp,
    )
}

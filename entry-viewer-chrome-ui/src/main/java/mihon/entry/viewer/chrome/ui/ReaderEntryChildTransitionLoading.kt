package mihon.entry.viewer.chrome.ui

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import mihon.presentation.kit.progress.LoadingIndicator
import mihon.presentation.kit.theme.KatariTheme

/**
 * Minimal progress affordance shown at a chapter boundary while its destination loads. It occupies
 * the slot the destination content will occupy, so no further interaction is needed once loading
 * completes.
 */
@Composable
fun ReaderEntryChildTransitionLoadingIndicator(
    loadingDescription: String,
    modifier: Modifier = Modifier,
    accentColor: Color = KatariTheme.colors.primary,
    indicatorSize: Dp = KatariTheme.sizes.iconLarge,
) {
    LoadingIndicator(
        modifier = modifier
            .size(indicatorSize)
            .semantics { contentDescription = loadingDescription },
        color = accentColor,
    )
}

package mihon.presentation.kit.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import mihon.presentation.kit.theme.KatariTheme

/**
 * Which of [count] swipeable pages is shown, such as the cover and art of an entry's cover viewer. [description] names
 * the page shown for accessibility services, since the dots alone say nothing.
 */
@Composable
fun PageDots(
    count: Int,
    current: Int,
    description: String,
    modifier: Modifier = Modifier,
) {
    val colors = KatariTheme.colors
    Row(
        modifier = modifier.semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
    ) {
        repeat(count) { index ->
            Box(
                modifier = Modifier
                    .size(PageDotSize)
                    .background(
                        color = if (index == current) colors.primary else colors.outlineVariant,
                        shape = KatariTheme.shapes.full,
                    ),
            )
        }
    }
}

private val PageDotSize = 8.dp

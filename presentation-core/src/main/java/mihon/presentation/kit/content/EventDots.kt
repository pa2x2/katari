package mihon.presentation.kit.content

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mihon.presentation.kit.theme.KatariTheme

/** How many events fall on a calendar day, as up to [max] small dots that each grow stronger than the one before. */
@Composable
fun EventDots(
    count: Int,
    modifier: Modifier = Modifier,
    max: Int = 3,
) {
    val color = KatariTheme.colors.primary
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xxs)) {
        repeat(count.coerceAtMost(max)) { index ->
            Box(
                modifier = Modifier
                    .size(EventDotSize)
                    .background(color.copy(alpha = (index + 1f) / max), KatariTheme.shapes.full),
            )
        }
    }
}

private val EventDotSize = 5.dp

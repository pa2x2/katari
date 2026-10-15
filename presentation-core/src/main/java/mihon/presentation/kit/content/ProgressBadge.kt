package mihon.presentation.kit.content

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.LocalContentColor
import mihon.presentation.kit.theme.color

/**
 * A small round mark pinned to a control, such as the translation mark on a chapter's download button. While the work
 * it stands for runs, [working] rings it: with [progress] (0 to 1) the ring fills, without it the ring spins.
 */
@Composable
fun ProgressBadge(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tone: ContentTone = ContentTone.Inherit,
    working: Boolean = false,
    progress: (() -> Float)? = null,
) {
    val tint = tone.color(fallback = LocalContentColor.current)
    Box(
        modifier = modifier
            .size(ProgressBadgeSize)
            .background(KatariTheme.colors.surface, KatariTheme.shapes.full),
        contentAlignment = Alignment.Center,
    ) {
        if (working) {
            if (progress != null) {
                CircularProgressIndicator(
                    progress = progress,
                    modifier = Modifier.size(ProgressBadgeSize),
                    color = tint,
                    strokeWidth = ProgressBadgeRingWidth,
                    trackColor = Color.Transparent,
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(ProgressBadgeSize),
                    color = tint,
                    strokeWidth = ProgressBadgeRingWidth,
                    trackColor = Color.Transparent,
                )
            }
        }
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(ProgressBadgeIconSize),
            tone = tone,
        )
    }
}

private val ProgressBadgeSize = 16.dp
private val ProgressBadgeIconSize = 10.dp
private val ProgressBadgeRingWidth = 1.5.dp

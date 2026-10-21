package mihon.presentation.kit.content

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.LocalContentColor
import mihon.presentation.kit.theme.TextRole
import mihon.presentation.kit.theme.color

/** An icon in the content colour or a [tone]. Size it with KatariTheme.sizes when 24dp does not fit. */
@Composable
fun Icon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tone: ContentTone = ContentTone.Inherit,
) {
    androidx.compose.material3.Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tone.color(fallback = LocalContentColor.current),
    )
}

@Composable
fun Icon(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tone: ContentTone = ContentTone.Inherit,
) {
    androidx.compose.material3.Icon(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tone.color(fallback = LocalContentColor.current),
    )
}

@Composable
fun Icon(
    bitmap: ImageBitmap,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tone: ContentTone = ContentTone.Inherit,
) {
    androidx.compose.material3.Icon(
        bitmap = bitmap,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tone.color(fallback = LocalContentColor.current),
    )
}

/** An icon inside a line of [role] text, such as a trend arrow before a figure or a chevron after a summary. */
@Composable
fun InlineIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    role: TextRole,
    modifier: Modifier = Modifier,
    tone: ContentTone = ContentTone.Inherit,
) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = modifier.size(role.lineHeight),
        tone = tone,
    )
}

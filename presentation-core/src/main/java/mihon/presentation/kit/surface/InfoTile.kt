package mihon.presentation.kit.surface

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Info
import mihon.presentation.kit.content.InlineIcon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole

/** A row of [InfoTile]s sharing the width and the tallest tile's height, such as an extension's version and language. */
@Composable
fun InfoTileRow(
    modifier: Modifier = Modifier,
    tiles: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(horizontal = KatariTheme.spacing.m),
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
        content = tiles,
    )
}

/**
 * One fact on a tile of its own: [value] above the [label] that names it. [onClick] makes the tile explain the fact,
 * such as what a content warning means, and marks it with an info icon.
 */
@Composable
fun RowScope.InfoTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    valueTone: ContentTone = ContentTone.Inherit,
    onClick: (() -> Unit)? = null,
) {
    val content: @Composable () -> Unit = {
        Column(modifier = Modifier.padding(horizontal = KatariTheme.spacing.l, vertical = KatariTheme.spacing.m)) {
            Text(
                text = value,
                role = TextRole.ItemTitle,
                tone = valueTone,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xxs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = label, role = TextRole.Supporting, tone = ContentTone.Secondary)
                if (onClick != null) {
                    InlineIcon(
                        imageVector = MaterialSymbols.Rounded.Info,
                        contentDescription = null,
                        role = TextRole.Supporting,
                        tone = ContentTone.Secondary,
                    )
                }
            }
        }
    }
    val tileModifier = modifier.weight(1f).fillMaxHeight()
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = tileModifier,
            container = SurfaceContainer.Default,
            shape = KatariTheme.shapes.large,
            content = content,
        )
    } else {
        Surface(
            modifier = tileModifier,
            container = SurfaceContainer.Default,
            shape = KatariTheme.shapes.large,
            content = content,
        )
    }
}

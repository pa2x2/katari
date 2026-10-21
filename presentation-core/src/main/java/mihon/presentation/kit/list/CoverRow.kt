package mihon.presentation.kit.list

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mihon.presentation.kit.content.LibraryMetrics
import mihon.presentation.kit.theme.KatariTheme

/**
 * A row led by an entry's cover, such as an update or a history item. [cover] draws the image filling the thumbnail
 * slot, [lines] are the title and details beside it, and [trailing] is an action at the end.
 *
 * A row without a [cover] continues the row above it, such as a chapter of an expanded group: its lines start where
 * the cover rows' lines do, and it sits tighter.
 */
@Composable
fun CoverRow(
    cover: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    lines: @Composable ColumnScope.() -> Unit,
) {
    val spacing = KatariTheme.spacing
    val nested = cover == null
    Row(
        modifier = modifier
            .then(if (selected) Modifier.background(KatariTheme.colors.selected) else Modifier)
            .then(
                if (onClick != null || onLongClick != null) {
                    Modifier.combinedClickable(onLongClick = onLongClick, onClick = { onClick?.invoke() })
                } else {
                    Modifier
                },
            )
            .padding(
                start = if (nested) spacing.m + LibraryMetrics.rowCover.width + spacing.l else spacing.m,
                end = spacing.m,
                top = if (nested) spacing.xs else spacing.s,
                bottom = if (nested) spacing.xs else spacing.s,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (cover != null) {
            Box(modifier = Modifier.size(LibraryMetrics.rowCover)) { cover() }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(
                    start = if (nested) KatariTheme.sizes.none else spacing.l,
                    end = spacing.l,
                ),
            content = lines,
        )
        trailing?.invoke()
    }
}

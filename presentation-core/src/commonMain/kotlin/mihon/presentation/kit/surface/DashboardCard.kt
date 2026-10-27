package mihon.presentation.kit.surface

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.list.RowGroupHeader
import mihon.presentation.kit.list.rowGroupEdgeInset
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole

/**
 * One section of a dashboard, such as the activity chart or the library's progress on Statistics. The [title] row can
 * end with a [note] on what the section covers, such as "Last 12 months", or with a text button for [actionLabel], such
 * as "See all". Figures and charts sit straight on the card, without tiles or cards of their own. A [footer] sits under
 * a line across the whole card, for a row that sums the section up and opens its details: build it from
 * [DashboardCardFooter]. The card keeps the side margin of row group cards, so a [RowGroupHeader] above it lines up with
 * its text; set it in a list without side padding.
 */
@Composable
fun DashboardCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    note: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val hasAction = actionLabel != null && onAction != null
    GroupCard(modifier.padding(horizontal = rowGroupEdgeInset()).fillMaxWidth()) {
        Column(Modifier.padding(KatariTheme.spacing.contentInset)) {
            if (title != null || note != null || hasAction) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (title != null) {
                        Text(text = title, modifier = Modifier.weight(1f), role = TextRole.SectionTitle)
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                    note?.let { Text(text = it, role = TextRole.Badge, tone = ContentTone.Secondary) }
                    if (hasAction) Button(label = actionLabel, onClick = onAction, style = ButtonStyle.Text)
                }
                Spacer(Modifier.height(KatariTheme.spacing.m))
            }
            content()
        }
        if (footer != null) {
            HorizontalDivider()
            footer()
        }
    }
}

/** The row at the bottom of a [DashboardCard] that sums the section up; tapping it opens the details it sums up. */
@Composable
fun DashboardCardFooter(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = KatariTheme.spacing.contentInset, vertical = KatariTheme.spacing.m),
        content = content,
    )
}

/**
 * One headline figure of a dashboard on a tile of its own, such as the time spent in a row of tiles above Statistics'
 * sections. It matches a [DashboardCard]'s fill and shape but leaves the side margin to the row it sits in.
 */
@Composable
fun DashboardTile(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    GroupCard(modifier) {
        Column(Modifier.padding(KatariTheme.spacing.contentInset), content = content)
    }
}

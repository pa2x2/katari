package app.katari.presentation.kit.surface

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import app.katari.presentation.kit.button.Button
import app.katari.presentation.kit.button.ButtonStyle
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.list.RowGroupHeader
import app.katari.presentation.kit.list.rowGroupEdgeInset
import app.katari.presentation.kit.pointer.clickCursor
import app.katari.presentation.kit.progress.CircularProgressIndicator
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole

/**
 * One section of a dashboard, such as the activity chart or the library's progress on Statistics. The [title] row can
 * end with a [note] on what the section covers, such as "Last 12 months", with a text button for [actionLabel], such
 * as "See all", or with icon buttons in [actions], such as moving to the period before. Figures and charts sit straight
 * on the card, without tiles or cards of their own. A [footer] sits under
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
    actions: (@Composable RowScope.() -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val hasAction = actionLabel != null && onAction != null
    GroupCard(modifier.padding(horizontal = rowGroupEdgeInset()).fillMaxWidth()) {
        Column(Modifier.padding(KatariTheme.spacing.contentInset)) {
            if (title != null || note != null || hasAction || actions != null) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (title != null) {
                        Text(text = title, modifier = Modifier.weight(1f), role = TextRole.SectionTitle)
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                    note?.let { Text(text = it, role = TextRole.Badge, tone = ContentTone.Secondary) }
                    if (hasAction) Button(label = actionLabel, onClick = onAction, style = ButtonStyle.Text)
                    actions?.invoke(this)
                }
                // Icon buttons already hold their own room around them.
                if (actions == null) Spacer(Modifier.height(KatariTheme.spacing.m))
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
            .clickCursor(enabled)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = KatariTheme.spacing.contentInset, vertical = KatariTheme.spacing.m),
        content = content,
    )
}

/**
 * One headline figure of a dashboard on a tile of its own, such as the time spent in a row of tiles above Statistics'
 * sections. It matches a [DashboardCard]'s fill and shape but leaves the side margin to the row it sits in. With
 * [onClick] it opens what the figure counts, such as a month's recap.
 */
@Composable
fun DashboardTile(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    GroupCard(modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickCursor().clickable(onClick = onClick) else Modifier)
                .padding(KatariTheme.spacing.contentInset),
            content = content,
        )
    }
}

/**
 * Progress toward a daily target on a strip of its own above a dashboard's sections, such as today's reading time
 * against the daily goal: a ring filled to [progress] (0 to 1), [title] and [supporting] beside it, and at its end a
 * related [figure] above its [figureLabel], such as the goal streak. It keeps a [DashboardCard]'s side margin.
 */
@Composable
fun DashboardGoalStrip(
    progress: Float,
    title: String,
    supporting: String,
    modifier: Modifier = Modifier,
    figure: String? = null,
    figureLabel: String? = null,
) {
    GroupCard(
        modifier = modifier.padding(horizontal = rowGroupEdgeInset()).fillMaxWidth(),
        container = SurfaceContainer.High,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = KatariTheme.spacing.contentInset,
                vertical = KatariTheme.spacing.m,
            ),
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.l),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(progress = { progress.coerceIn(0f, 1f) })
            Column(Modifier.weight(1f)) {
                Text(text = title, role = TextRole.SectionTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = supporting, role = TextRole.Supporting, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (figure != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = figure, role = TextRole.SectionTitle, maxLines = 1)
                    figureLabel?.let { Text(text = it, role = TextRole.Supporting, maxLines = 1) }
                }
            }
        }
    }
}

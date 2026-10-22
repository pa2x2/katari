package mihon.presentation.kit.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.OpenInNew
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.list.RowGroup
import mihon.presentation.kit.list.SettingsRow
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole

/** A page outside the app that a details sheet points to, such as an engine's documentation. */
class DetailsLink(
    val label: String,
    val onClick: () -> Unit,
    val supportingLabel: String? = null,
)

/**
 * Explains one item, such as an engine or a library: optional [artwork], a title and [subtitle], an optional [status]
 * line, then its sections ([DetailsSection]) together in one card and its [links] as rows in a second. It closes like
 * every other sheet, so it has no close button.
 */
@Composable
fun DetailsSheet(
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    artwork: (@Composable () -> Unit)? = null,
    status: (@Composable () -> Unit)? = null,
    links: List<DetailsLink> = emptyList(),
    content: @Composable ColumnScope.() -> Unit,
) {
    val spacing = KatariTheme.spacing
    Sheet(onDismissRequest = onDismissRequest, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = spacing.xl),
            verticalArrangement = Arrangement.spacedBy(spacing.l),
        ) {
            Column(
                modifier = Modifier.padding(start = spacing.rowInset, end = spacing.rowInset, top = spacing.xl),
                verticalArrangement = Arrangement.spacedBy(spacing.l),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.l),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    artwork?.invoke()
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                        Text(title, role = TextRole.SheetTitle)
                        if (subtitle != null) Text(subtitle, role = TextRole.Body, tone = ContentTone.Secondary)
                    }
                }
                status?.invoke()
            }
            RowGroup(
                rows = listOf<@Composable () -> Unit> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(spacing.l),
                        verticalArrangement = Arrangement.spacedBy(spacing.l),
                        content = content,
                    )
                },
            )
            if (links.isNotEmpty()) {
                RowGroup(
                    rows = links.map<DetailsLink, @Composable () -> Unit> { link ->
                        {
                            SettingsRow(
                                title = link.label,
                                subtitle = link.supportingLabel,
                                trailing = {
                                    Icon(
                                        MaterialSymbols.AutoMirroredRounded.OpenInNew,
                                        contentDescription = null,
                                        tone = ContentTone.Secondary,
                                    )
                                },
                                onClick = link.onClick,
                            )
                        }
                    },
                )
            }
        }
    }
}

/** A titled paragraph in a details sheet. */
@Composable
fun DetailsSection(title: String, body: String, modifier: Modifier = Modifier) {
    DetailsSection(title, modifier) {
        Text(body, role = TextRole.Body, tone = ContentTone.Secondary)
    }
}

@Composable
fun DetailsSection(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs)) {
        Text(title, role = TextRole.SectionTitle)
        content()
    }
}

package eu.kanade.tachiyomi.ui.browse.source.browse.filter.paged

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.navigation.UpButton
import mihon.presentation.kit.refresh.RefreshButton
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Back to the sheet, the list's name, and how many of its items are selected. */
@Composable
internal fun PagedGroupFilterHeader(
    title: String,
    selectedCount: Int,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    refreshEnabled: Boolean,
) {
    val spacing = KatariTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // The end inset is sized for the refresh button, whose touch area pads its icon.
            .padding(start = spacing.xs, end = spacing.xs, top = spacing.s, bottom = spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UpButton(onClick = onBack)
        Column(
            Modifier
                .weight(1f)
                .padding(start = spacing.xs),
        ) {
            Text(
                text = title,
                role = TextRole.SectionTitle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(MR.strings.browse_filter_selected_count, selectedCount),
                role = TextRole.Supporting,
            )
        }
        RefreshButton(onClick = onRefresh, enabled = refreshEnabled)
    }
}

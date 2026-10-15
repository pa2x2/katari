package eu.kanade.presentation.more.settings.screen.data

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.presentation.kit.list.SectionHeader
import mihon.presentation.kit.surface.Card
import mihon.presentation.kit.surface.CardStyle
import mihon.presentation.kit.theme.KatariTheme

/** A card grouping backup or restore options, under an optional section header. */
@Composable
internal fun BackupSectionCard(
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column {
        if (title != null) SectionHeader(title)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = KatariTheme.spacing.contentInset, vertical = KatariTheme.spacing.s),
            style = CardStyle.Elevated,
        ) {
            Column(modifier = Modifier.padding(vertical = KatariTheme.spacing.s), content = content)
        }
    }
}

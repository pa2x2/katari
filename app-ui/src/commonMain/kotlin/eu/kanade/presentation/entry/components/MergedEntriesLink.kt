package eu.kanade.presentation.entry.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.KeyboardArrowRight
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.util.clickableNoIndication

@Composable
internal fun MergedEntriesLink(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.clickableNoIndication(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = EntryActionIcons.merge,
            contentDescription = null,
            modifier = Modifier.size(KatariTheme.sizes.iconSmall),
            tone = ContentTone.Primary,
        )
        Text(
            text = pluralStringResource(MR.plurals.entry_merged_entries_count, count, count),
            role = TextRole.Body,
            tone = ContentTone.Primary,
        )
        Icon(
            imageVector = MaterialSymbols.AutoMirroredRounded.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(KatariTheme.sizes.iconSmall),
            tone = ContentTone.Primary,
        )
    }
}

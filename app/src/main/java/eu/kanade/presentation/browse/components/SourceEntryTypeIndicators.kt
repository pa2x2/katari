package eu.kanade.presentation.browse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.entry.InlineEntryTypeIndicator
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun SourceEntryTypeIndicators(
    supportedEntryTypes: Set<EntryType>?,
    modifier: Modifier = Modifier,
) {
    if (supportedEntryTypes.isNullOrEmpty()) return

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EntryType.entries
            .filter(supportedEntryTypes::contains)
            .forEach { entryType ->
                val typeName = stringResource(entryType.entryTypePresentation().displayNameLabel)
                entryType.InlineEntryTypeIndicator(
                    contentDescription = stringResource(MR.strings.source_supplies_entry_type, typeName),
                )
            }
    }
}

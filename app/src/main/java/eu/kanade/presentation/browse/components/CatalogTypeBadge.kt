package eu.kanade.presentation.browse.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.presentation.core.components.Badge

@Composable
fun CatalogTypeBadge(entryType: EntryType, modifier: Modifier = Modifier) {
    Badge(
        imageVector = entryType.entryTypePresentation().badgeIcon,
        modifier = modifier,
    )
}

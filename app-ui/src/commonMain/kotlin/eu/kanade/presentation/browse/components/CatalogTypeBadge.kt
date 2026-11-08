package eu.kanade.presentation.browse.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.katari.presentation.kit.content.Badge
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.tachiyomi.source.entry.EntryType

@Composable
fun CatalogTypeBadge(entryType: EntryType, modifier: Modifier = Modifier) {
    Badge(
        icon = entryType.entryTypePresentation().badgeIcon,
        modifier = modifier,
    )
}

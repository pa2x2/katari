package eu.kanade.presentation.library.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import eu.kanade.presentation.browse.components.CatalogTypeBadge
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.Badge
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun DownloadsBadge(count: Int) {
    if (count > 0) {
        Badge(
            text = "$count",
            color = MaterialTheme.colorScheme.tertiary,
            textColor = MaterialTheme.colorScheme.onTertiary,
        )
    }
}

@Composable
internal fun UnreadBadge(count: Long) {
    if (count > 0) {
        Badge(text = "$count")
    }
}

@Composable
internal fun LocalBadge(isLocal: Boolean) {
    if (isLocal) {
        Badge(
            imageVector = Icons.Outlined.Folder,
            color = MaterialTheme.colorScheme.tertiary,
            iconColor = MaterialTheme.colorScheme.onTertiary,
        )
    }
}

@Composable
internal fun LanguageBadge(sourceLanguage: String) {
    if (sourceLanguage.isNotEmpty()) {
        Badge(
            text = if (sourceLanguage == LibraryItem.MULTI_SOURCE_ID.toString()) {
                stringResource(MR.strings.multi_lang)
            } else {
                sourceLanguage.uppercase()
            },
            color = MaterialTheme.colorScheme.tertiary,
            textColor = MaterialTheme.colorScheme.onTertiary,
        )
    }
}

@Composable
internal fun EntryTypeBadge(entryType: EntryType) {
    CatalogTypeBadge(entryType = entryType)
}

@PreviewLightDark
@Composable
private fun BadgePreview() {
    TachiyomiPreviewTheme {
        Column {
            DownloadsBadge(count = 10)
            UnreadBadge(count = 10)
            LocalBadge(isLocal = true)
            LanguageBadge(sourceLanguage = "EN")
            EntryTypeBadge(entryType = EntryType.MANGA)
        }
    }
}

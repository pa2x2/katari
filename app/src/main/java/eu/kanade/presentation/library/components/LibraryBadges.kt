package eu.kanade.presentation.library.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import eu.kanade.presentation.browse.components.CatalogTypeBadge
import eu.kanade.presentation.entry.components.EntryActionIcons
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Download
import mihon.icons.materialsymbols.rounded.Folder
import mihon.presentation.kit.content.Badge
import mihon.presentation.kit.content.BadgeTone
import mihon.presentation.kit.theme.KatariPreviewTheme
import tachiyomi.domain.library.model.LibraryItem

@Composable
internal fun DownloadsBadge(count: Int) {
    if (count > 0) {
        Badge(
            text = "$count",
            icon = MaterialSymbols.Rounded.Download,
            tone = BadgeTone.Neutral,
        )
    }
}

@Composable
internal fun UnreadBadge(count: Long) {
    if (count > 0) {
        Badge(
            text = "$count",
            tone = BadgeTone.Primary,
        )
    }
}

@Composable
internal fun LocalBadge(isLocal: Boolean) {
    if (isLocal) {
        Badge(
            icon = MaterialSymbols.Rounded.Folder,
            tone = BadgeTone.Tertiary,
        )
    }
}

@Composable
internal fun LanguageBadge(languages: List<String>) {
    if (languages.isNotEmpty()) {
        Badge(
            text = languageBadgeText(languages),
            tone = BadgeTone.Tertiary,
        )
    }
}

/** Names up to two languages and counts the rest, so a cross-source merge's badge stays cover-sized. */
internal fun languageBadgeText(languages: List<String>): String {
    val shown = languages.take(MAX_BADGE_LANGUAGES).joinToString(" · ") { it.uppercase() }
    val hidden = languages.size - MAX_BADGE_LANGUAGES
    return if (hidden > 0) "$shown +$hidden" else shown
}

private const val MAX_BADGE_LANGUAGES = 2

@Composable
internal fun MergedBadge(memberCount: Int) {
    Badge(
        text = "$memberCount",
        icon = EntryActionIcons.merge,
    )
}

@Composable
internal fun EntryTypeBadge(entryType: EntryType) {
    CatalogTypeBadge(entryType = entryType)
}

/** Progress badges: the unread count leads, downloads follow with their own glyph so the two never read alike. */
@Composable
internal fun LibraryItemProgressBadges(item: LibraryItem, settings: LibraryDisplaySettings) {
    if (settings.unreadBadge) {
        item.unconsumedCount?.let { UnreadBadge(count = it) }
    }
    if (settings.downloadBadge) {
        DownloadsBadge(count = item.downloadCount)
    }
}

/** Origin badges: entry type, merge, local source and language. */
@Composable
internal fun LibraryItemOriginBadges(item: LibraryItem, settings: LibraryDisplaySettings) {
    if (settings.entryTypeBadge) {
        EntryTypeBadge(entryType = item.entry.type)
    }
    if (settings.mergedBadge && item.isMerged) {
        MergedBadge(memberCount = item.memberEntries.size)
    }
    if (settings.localBadge) {
        LocalBadge(isLocal = item.isLocal)
    }
    if (settings.languageBadge) {
        LanguageBadge(languages = item.languages)
    }
}

@PreviewLightDark
@Composable
private fun BadgePreview() {
    KatariPreviewTheme {
        Column {
            DownloadsBadge(count = 10)
            UnreadBadge(count = 10)
            LocalBadge(isLocal = true)
            LanguageBadge(languages = listOf("en"))
            MergedBadge(memberCount = 6)
            EntryTypeBadge(entryType = EntryType.MANGA)
        }
    }
}

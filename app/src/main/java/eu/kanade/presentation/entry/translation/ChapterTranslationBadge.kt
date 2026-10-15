package eu.kanade.presentation.entry.translation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.entry.interactions.translate.EntryTranslateStatus
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Translate
import mihon.presentation.kit.content.ProgressBadge
import mihon.presentation.kit.theme.ContentTone

/** The 文A mark a chapter's download indicator carries when the chapter is translated or on its way to it. */
@Composable
fun ChapterTranslationBadge(
    status: EntryTranslateStatus,
    modifier: Modifier = Modifier,
) {
    val progress = (status as? EntryTranslateStatus.Translating)?.progress
    ProgressBadge(
        icon = MaterialSymbols.Rounded.Translate,
        contentDescription = entryTranslateStatusText(status),
        modifier = modifier,
        tone = when (status) {
            EntryTranslateStatus.Translated, is EntryTranslateStatus.Translating -> ContentTone.Primary
            EntryTranslateStatus.WaitingForDownload, EntryTranslateStatus.Queued -> ContentTone.Secondary
            is EntryTranslateStatus.Failed -> ContentTone.Error
        },
        working = progress != null,
        progress = progress?.takeIf { it.total > 0 }?.let { { it.done.toFloat() / it.total } },
    )
}

package eu.kanade.presentation.entry.translation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.katari.entry.interactions.translate.EntryTranslateStatus
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Translate
import app.katari.presentation.kit.content.ProgressBadge
import app.katari.presentation.kit.theme.ContentTone

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

package eu.kanade.presentation.entry.components

import androidx.compose.runtime.Composable
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.tachiyomi.ui.entry.chapters.EntryChapterList
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.sheet.ChapterSheet
import tachiyomi.domain.entry.model.Entry
import tachiyomi.presentation.core.i18n.stringResource

/** What a source says about [item], opened from its row's still, with the action that opens it. */
@Composable
fun EntryChapterSheet(
    entry: Entry,
    item: EntryChapterList.Item,
    onDismissRequest: () -> Unit,
    onOpen: (() -> Unit)?,
) {
    val presentation = item.entry.type.entryTypePresentation()
    ChapterSheet(
        title = entryChapterTitle(entry, item.chapter),
        onDismissRequest = onDismissRequest,
        action = DialogAction(
            label = stringResource(presentation.openChildLabel),
            onClick = {
                onDismissRequest()
                onOpen?.invoke()
            },
            enabled = onOpen != null,
        ),
        kind = entryChapterKindLabel(item),
        date = entryChapterDate(item.chapter),
        scanlator = item.chapter.scanlator?.takeIf { it.isNotBlank() },
        summary = item.chapter.summary,
        preview = entryChapterPreview(item),
    )
}

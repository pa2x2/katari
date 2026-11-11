package eu.kanade.presentation.library.update.report.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextOverflow
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.KeyboardArrowDown
import app.katari.icons.materialsymbols.rounded.KeyboardArrowUp
import app.katari.library.update.report.LibraryUpdateReport
import app.katari.presentation.kit.content.InlineIcon
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.content.TonedSpan
import app.katari.presentation.kit.content.TonedText
import app.katari.presentation.kit.list.CoverRow
import app.katari.presentation.kit.theme.TextRole
import eu.kanade.presentation.entry.entryTypePresentation
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/**
 * An entry the update found new chapters for, as Updates shows an entry's updates of one day: one chapter is named and
 * opens; several are counted, and the row shows or hides them. The cover opens the entry.
 *
 * @param chapters the chapters found; empty when they can't be told apart any more, such as after a refresh, and the
 * row only counts them.
 */
@Composable
internal fun NewChaptersRow(
    item: LibraryUpdateReport.Item,
    chapters: List<EntryChapter>,
    sourceName: String?,
    onClickEntry: (Entry) -> Unit,
    onOpenChapter: (Entry, EntryChapter) -> Unit,
) {
    val entry = item.entry
    val single = chapters.singleOrNull()
    if (chapters.size <= 1) {
        ReportRow(
            entry = entry,
            title = entry.title,
            subtitle = item.withRecheck(
                single?.name ?: pluralStringResource(
                    MR.plurals.library_updates_new_chapters,
                    item.status.newChapters,
                    item.status.newChapters,
                ),
            ),
            sourceName = sourceName,
            onClick = { if (single != null) onOpenChapter(entry, single) else onClickEntry(entry) },
            onClickCover = { onClickEntry(entry) }.takeIf { single != null },
        )
        return
    }
    var expanded by rememberSaveable(entry.id) { mutableStateOf(false) }
    Column {
        CoverRow(
            cover = { ReportCover(entry, onClick = { onClickEntry(entry) }) },
            onClick = { expanded = !expanded },
        ) {
            Text(text = entry.title, role = TextRole.ItemTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            ReportEntryDetails(entry) {
                TonedText(
                    spans = listOfNotNull(
                        TonedSpan(
                            item.withRecheck(
                                pluralStringResource(
                                    entry.type.entryTypePresentation().childCountPlural,
                                    chapters.size,
                                    chapters.size.toString(),
                                ),
                            ),
                        ),
                        sourceName?.let { TonedSpan(it) },
                    ),
                    role = TextRole.Supporting,
                    separator = " · ",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                InlineIcon(
                    imageVector = if (expanded) {
                        MaterialSymbols.Rounded.KeyboardArrowUp
                    } else {
                        MaterialSymbols.Rounded.KeyboardArrowDown
                    },
                    contentDescription = stringResource(
                        if (expanded) MR.strings.action_collapse else MR.strings.action_expand,
                    ),
                    role = TextRole.Supporting,
                )
            }
        }
        if (expanded) {
            chapters.forEach { chapter ->
                CoverRow(cover = null, onClick = { onOpenChapter(entry, chapter) }) {
                    Text(
                        text = chapter.name,
                        role = TextRole.Supporting,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

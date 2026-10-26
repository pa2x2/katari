package mihon.entry.interactions.book.document.reader.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.list.RowGroupHeader
import mihon.presentation.kit.list.RowGroupItem
import mihon.presentation.kit.list.RowGroupPosition
import mihon.presentation.kit.list.SearchResultRow
import mihon.presentation.kit.list.SettingsRow
import mihon.presentation.kit.sheet.Sheet
import mihon.presentation.kit.sheet.SheetHeader
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/** Searches the book's downloaded chapters for [query] and lists what it finds, chapter by chapter, as it goes. */
@Composable
internal fun BookDocumentBookSearchSheet(
    query: String,
    bookSearch: BookDocumentBookSearch,
    onResultClick: (BookDocumentBookSearchResult) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var progress by remember(query) { mutableStateOf<BookDocumentBookSearchProgress?>(null) }
    LaunchedEffect(query) {
        bookSearch.search(query).collect { progress = it }
    }
    Sheet(onDismissRequest = onDismissRequest, capHeight = true) {
        Column {
            SheetHeader(title = stringResource(MR.strings.book_reader_search_all_chapters))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                contentPadding = PaddingValues(bottom = KatariTheme.spacing.m),
            ) {
                item(key = STATUS_ROW) {
                    SettingsRow(
                        title = progress.statusText(),
                        subtitle = stringResource(MR.strings.book_reader_search_all_downloaded),
                    )
                }
                progress?.chapters.orEmpty().forEach { chapter ->
                    item(key = "chapter:${chapter.chapter.id}") {
                        RowGroupHeader(
                            text = chapter.chapter.name,
                            trailing = {
                                Text(
                                    pluralStringResource(
                                        MR.plurals.book_reader_search_matches,
                                        chapter.matchCount,
                                        chapter.matchCount,
                                    ),
                                    role = TextRole.Supporting,
                                    tone = ContentTone.Secondary,
                                )
                            },
                        )
                    }
                    itemsIndexed(
                        chapter.results,
                        key = { _, result ->
                            "result:${chapter.chapter.id}:${result.resourceId}:${result.match.start}"
                        },
                    ) { index, result ->
                        RowGroupItem(position = RowGroupPosition.of(index, chapter.results.size)) {
                            SearchResultRow(
                                excerpt = result.excerpt.text,
                                matchStart = result.excerpt.matchStart,
                                matchEnd = result.excerpt.matchEnd,
                                onClick = { onResultClick(result) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookDocumentBookSearchProgress?.statusText(): String = when {
    this == null -> stringResource(MR.strings.book_reader_search_all_starting)
    !done -> stringResource(MR.strings.book_reader_search_all_searching, searched + 1, total)
    chapters.isEmpty() -> stringResource(MR.strings.book_reader_search_all_none)
    else -> stringResource(
        MR.strings.book_reader_search_all_found,
        pluralStringResource(MR.plurals.book_reader_search_matches, matchCount, matchCount),
        pluralStringResource(MR.plurals.book_reader_search_chapters, chapters.size, chapters.size),
    )
}

private const val STATUS_ROW = "status"

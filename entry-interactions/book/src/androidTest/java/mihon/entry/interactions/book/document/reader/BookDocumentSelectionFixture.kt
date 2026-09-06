package mihon.entry.interactions.book.document.reader

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import mihon.book.api.document.BookDocumentBlock
import mihon.book.api.document.BookDocumentBlockContent
import mihon.book.api.document.BookDocumentBlockId
import mihon.book.api.document.BookDocumentBlockKind
import mihon.book.api.document.BookDocumentBlockRole
import mihon.book.api.document.BookDocumentRichText
import mihon.book.api.document.BookDocumentTextRange
import mihon.entry.interactions.book.document.reader.settings.BookDocumentReaderThemeMode
import mihon.entry.interactions.book.document.reader.theme.LocalBookDocumentReaderPalette
import mihon.entry.interactions.book.document.reader.theme.bookDocumentReaderPalette

@Composable
internal fun BookDocumentSelectionFixture(
    text: String,
    showTextSelectionMenu: Boolean = false,
    onSelection: (BookDocumentTextSelection) -> Unit,
    onSession: (BookDocumentChapterSelection, ScrollState) -> Unit = { _, _ -> },
) {
    val block = BookDocumentBlock(
        id = BookDocumentBlockId("paragraph"),
        role = BookDocumentBlockRole(BookDocumentBlockKind.PARAGRAPH),
        content = BookDocumentBlockContent.Text(
            BookDocumentRichText(text, BookDocumentTextRange(0, text.length)),
        ),
        plainText = text,
        sourceFragments = emptyList(),
        logicalStart = 0,
        logicalEndExclusive = text.length,
    )
    val scroll = rememberScrollState()
    MaterialTheme {
        CompositionLocalProvider(
            LocalBookDocumentReaderPalette provides bookDocumentReaderPalette(BookDocumentReaderThemeMode.APP),
            LocalBookDocumentTextInteraction provides BookDocumentTextInteraction.Disabled.copy(
                observeSelections = true,
                showTextSelectionMenu = showTextSelectionMenu,
                onSelection = onSelection,
            ),
            LocalBookDocumentSelectionChapterId provides 1L,
        ) {
            BookDocumentChapterSelectionContainer(chapterId = 1L) { selection ->
                onSession(selection, scroll)
                Box(Modifier.fillMaxSize().verticalScroll(scroll)) {
                    BookDocumentSelectableText(
                        text = text,
                        links = emptyList(),
                        inlineStyles = emptyList(),
                        identity = "paragraph",
                        block = block,
                        separatorAfter = "\n",
                        onAnchorClick = {},
                        onExternalLinkClick = {},
                    )
                }
            }
        }
    }
}

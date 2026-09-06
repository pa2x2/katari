package mihon.entry.interactions.book.document.reader

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mihon.book.api.document.BookDocumentLinkTarget
import tachiyomi.domain.entry.model.EntryChapter

/** A separate viewport/selection lifetime using the same semantic blocks and position restoration as the reader. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BookDocumentReferenceSheet(
    section: BookDocumentSection<EntryChapter>,
    onDismiss: () -> Unit,
    onLink: (BookDocumentSection<EntryChapter>, BookDocumentLinkTarget) -> Unit,
    onExternalLink: (String) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        key(section.key) {
            val listState = rememberLazyListState()
            LaunchedEffect(section.initialPosition, LocalBookDocumentTextScale.current) {
                listState.scrollToBookDocumentPosition(
                    section.document,
                    section.initialPosition,
                    section.viewerBlockIndex(section.initialPosition.blockId),
                )
            }
            BookDocumentChapterSelectionContainer(
                chapterId = section.owner.id,
                ownerIdentity = "book-document-reference:${section.key}",
            ) {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(bottom = 24.dp)) {
                    items(section.viewerBlocks, key = { it.key }) { item ->
                        BookDocumentViewerBlock(item, onLink, onExternalLink, onReaderTap = {})
                    }
                }
            }
        }
    }
}

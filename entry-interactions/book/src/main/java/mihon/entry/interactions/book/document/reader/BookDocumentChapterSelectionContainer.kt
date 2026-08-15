package mihon.entry.interactions.book.document.reader

import android.content.ClipData
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.text.contextmenu.builder.item
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuKeys
import androidx.compose.foundation.text.contextmenu.modifier.appendTextContextMenuComponents
import androidx.compose.foundation.text.contextmenu.modifier.filterTextContextMenuComponents
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.SelectionState
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.toClipEntry
import kotlinx.coroutines.launch
import mihon.entry.interactions.book.R
import mihon.entry.interactions.book.document.reader.theme.LocalBookDocumentReaderPalette

/** Binds the active chapter's selection owner to Compose selection and Android context actions. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun BookDocumentChapterSelectionContainer(
    chapterId: Long,
    modifier: Modifier = Modifier,
    content: @Composable (BookDocumentChapterSelection) -> Unit,
) {
    val selectionState = remember { SelectionState() }
    val interaction = LocalBookDocumentTextInteraction.current
    val palette = LocalBookDocumentReaderPalette.current
    val selectionColors = remember(palette.accent) {
        TextSelectionColors(
            handleColor = palette.accent,
            backgroundColor = palette.accent.copy(alpha = 0.4f),
        )
    }
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    val session = remember(selectionState) {
        BookDocumentChapterSelection(
            ownerIdentity = "book-document-chapter-$chapterId",
            selectionState = selectionState,
        )
    }
    SideEffect {
        session.interaction = interaction
        session.updateOwnerIdentity("book-document-chapter-$chapterId")
    }

    val selectedTexts = selectionState.selectedTexts
    val layoutRevision = session.layoutRevision
    val projection = session.project(selectedTexts, interaction.rootPositionInWindow)
    val copyLabel = androidx.compose.ui.res.stringResource(android.R.string.copy)
    val listenLabel = androidx.compose.ui.res.stringResource(
        if (projection?.identity == interaction.activeSpeechSelectionIdentity) {
            R.string.book_reader_selection_stop_listening
        } else {
            R.string.book_reader_selection_listen
        },
    )
    val translateLabel = androidx.compose.ui.res.stringResource(R.string.book_reader_selection_translate)

    LaunchedEffect(
        selectedTexts,
        layoutRevision,
        interaction.rootPositionInWindow,
        interaction.observeSelections,
    ) {
        if (selectedTexts.isNotEmpty() && projection == null) {
            session.clearSelection()
            session.clearPublishedSelection()
            return@LaunchedEffect
        }
        session.publishSelection(projection)
    }
    DisposableEffect(session) {
        onDispose {
            session.clearSelection()
            session.clearPublishedSelection()
        }
    }

    CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
        SelectionContainer(
            state = selectionState,
            modifier = modifier
                .captureSelectionAtPointerDown(session)
                .filterTextContextMenuComponents { component ->
                    interaction.showTextSelectionMenu && component.key !== TextContextMenuKeys.CopyKey
                }
                .appendTextContextMenuComponents {
                    projection ?: return@appendTextContextMenuComponents
                    item(BookDocumentCopySelectionKey, copyLabel) {
                        coroutineScope.launch {
                            clipboard.setClipEntry(
                                ClipData.newPlainText(copyLabel, projection.text).toClipEntry(),
                            )
                        }
                        session.clearSelection()
                        close()
                    }
                    val actions = interaction.selectionActions
                    if (interaction.observeSelections && BookDocumentSelectionAction.Listen in actions) {
                        item(BookDocumentListenSelectionKey, listenLabel) {
                            session.performAction(BookDocumentSelectionAction.Listen)
                            close()
                        }
                    }
                    if (interaction.observeSelections && BookDocumentSelectionAction.Translate in actions) {
                        item(BookDocumentTranslateSelectionKey, translateLabel) {
                            session.performAction(BookDocumentSelectionAction.Translate)
                            close()
                        }
                    }
                },
        ) {
            CompositionLocalProvider(LocalBookDocumentChapterSelection provides session) {
                content(session)
            }
        }
    }
}

private fun Modifier.captureSelectionAtPointerDown(
    session: BookDocumentChapterSelection,
): Modifier = pointerInput(session) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        session.captureSelectionAtPointerDown()
        do {
            val event = awaitPointerEvent(PointerEventPass.Initial)
        } while (event.changes.any { it.pressed })
    }
}

private data object BookDocumentCopySelectionKey
private data object BookDocumentListenSelectionKey
private data object BookDocumentTranslateSelectionKey

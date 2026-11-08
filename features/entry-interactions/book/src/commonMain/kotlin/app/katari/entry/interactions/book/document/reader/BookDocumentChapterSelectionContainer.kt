package app.katari.entry.interactions.book.document.reader

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import app.katari.entry.interactions.book.document.reader.input.secondaryClicks
import app.katari.entry.interactions.book.document.reader.theme.LocalBookDocumentReaderPalette
import app.katari.presentation.kit.menu.ContextMenu
import app.katari.presentation.kit.menu.DropdownMenuItem
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Binds the active chapter's selection owner to Compose selection and its actions: on Android through the floating
 * toolbar, where the platform's own actions follow the reader's; on the desktop, which has no such toolbar and whose
 * Compose shows its own short menu of the text's, through the reader's menu on a right click.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun BookDocumentChapterSelectionContainer(
    chapterId: Long,
    modifier: Modifier = Modifier,
    ownerIdentity: String = "book-document-chapter-$chapterId",
    content: @Composable (BookDocumentChapterSelection) -> Unit,
) {
    val selectionState = remember { SelectionState() }
    val interaction = LocalBookDocumentTextInteraction.current
    val palette = LocalBookDocumentReaderPalette.current
    val selectionColors = remember(palette.accent) {
        TextSelectionColors(handleColor = palette.accent, backgroundColor = palette.selection)
    }
    val session = remember(selectionState) {
        BookDocumentChapterSelection(
            ownerIdentity = ownerIdentity,
            selectionState = selectionState,
        )
    }
    SideEffect {
        session.interaction = interaction
        session.updateOwnerIdentity(ownerIdentity)
    }

    val selectedTexts = selectionState.selectedTexts
    val layoutRevision = session.layoutRevision
    val projection = session.project(selectedTexts, interaction.rootPositionInWindow)
    val copyLabel = stringResource(MR.strings.action_copy)
    val listenLabel = stringResource(
        if (projection?.identity == interaction.activeSpeechSelectionIdentity) {
            MR.strings.book_reader_selection_stop_listening
        } else {
            MR.strings.book_reader_selection_listen
        },
    )
    val translateLabel = stringResource(MR.strings.book_reader_selection_translate)
    val hideTextLabel = stringResource(MR.strings.book_reader_hide_text)

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

    // The reader's actions on the selection, which the platform's menu lists first or its own right-click menu shows
    val menuItems = projection?.let {
        buildList {
            add(
                BookDocumentSelectionMenuItem(BookDocumentSelectionMenuKey.Copy, copyLabel) {
                    interaction.copyText(copyLabel, projection.text)
                    session.clearSelection()
                },
            )
            val actions = interaction.selectionActions
            if (interaction.observeSelections && BookDocumentSelectionAction.Listen in actions) {
                add(
                    BookDocumentSelectionMenuItem(BookDocumentSelectionMenuKey.Listen, listenLabel) {
                        session.performAction(BookDocumentSelectionAction.Listen)
                    },
                )
            }
            if (interaction.observeSelections && BookDocumentSelectionAction.Translate in actions) {
                add(
                    BookDocumentSelectionMenuItem(BookDocumentSelectionMenuKey.Translate, translateLabel) {
                        session.performAction(BookDocumentSelectionAction.Translate)
                    },
                )
            }
            interaction.onHideText?.let { hideText ->
                add(
                    BookDocumentSelectionMenuItem(BookDocumentSelectionMenuKey.HideText, hideTextLabel) {
                        hideText(projection.text)
                        session.clearSelection()
                    },
                )
            }
        }
    }.orEmpty()
    var rightClickMenu by remember { mutableStateOf<Offset?>(null) }

    ProvideBookDocumentSelectionSettlementToolbar(session, modifier) {
        CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
            Box {
                SelectionContainer(
                    state = selectionState,
                    modifier = Modifier
                        .captureSelectionAtPointerDown(
                            session,
                            settlesOnRelease = interaction.selectionMenuOnRightClick,
                        )
                        .then(
                            if (interaction.selectionMenuOnRightClick) {
                                // Ahead of the text inside, so the selection's plain Copy menu never opens; the
                                // links and pictures inside still see the press and open menus of their own
                                Modifier.secondaryClicks(PointerEventPass.Initial) { position ->
                                    if (session.hasSelection) rightClickMenu = position
                                    true
                                }
                            } else {
                                Modifier
                            },
                        )
                        .filterTextContextMenuComponents { component ->
                            interaction.showTextSelectionMenu && component.key !== TextContextMenuKeys.CopyKey
                        }
                        .appendTextContextMenuComponents {
                            menuItems.forEach { menuItem ->
                                item(menuItem.key, menuItem.label) {
                                    menuItem.run()
                                    close()
                                }
                            }
                        },
                ) {
                    CompositionLocalProvider(LocalBookDocumentChapterSelection provides session) {
                        content(session)
                    }
                }
                val menuPosition = rightClickMenu
                if (menuPosition != null && menuItems.isNotEmpty()) {
                    ContextMenu(position = menuPosition, onDismissRequest = { rightClickMenu = null }) {
                        menuItems.forEach { menuItem ->
                            DropdownMenuItem(label = menuItem.label, onClick = menuItem.run)
                        }
                    }
                }
            }
        }
    }
}

/** One of the reader's own actions on the selection. */
private class BookDocumentSelectionMenuItem(
    val key: BookDocumentSelectionMenuKey,
    val label: String,
    val run: () -> Unit,
)

/**
 * Notes whether there was a selection as a press starts. With [settlesOnRelease], where no floating toolbar coming up
 * says a selection is done, the selection also counts as settled once the press that made it lets go.
 */
private fun Modifier.captureSelectionAtPointerDown(
    session: BookDocumentChapterSelection,
    settlesOnRelease: Boolean,
): Modifier = pointerInput(session, settlesOnRelease) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        session.captureSelectionAtPointerDown()
        if (settlesOnRelease) session.setSelectionSettled(false)
        do {
            val event = awaitPointerEvent(PointerEventPass.Initial)
        } while (event.changes.any { it.pressed })
        if (settlesOnRelease) session.setSelectionSettled(true)
    }
}

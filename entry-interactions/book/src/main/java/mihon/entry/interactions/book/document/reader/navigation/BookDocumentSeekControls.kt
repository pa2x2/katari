package mihon.entry.interactions.book.document.reader.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import mihon.book.api.BookLocator
import mihon.entry.interactions.book.R
import mihon.entry.interactions.book.document.reader.BookDocumentReaderState
import mihon.entry.interactions.reader.settings.BookDocumentReadingMode
import mihon.entry.viewer.chrome.ui.ReaderPageNavigatorType
import mihon.entry.viewer.chrome.ui.ReaderPositionNavigator
import mihon.entry.viewer.chrome.ui.ReaderReturnPositionBar
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.surface.Pill
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import kotlin.math.roundToInt
import tachiyomi.presentation.core.i18n.stringResource as sharedStringResource

/** Manga-familiar controls with a local preview draft, committed only on release or explicit input. */
@Composable
internal fun BookDocumentSeekControls(
    seekState: BookDocumentSeekState,
    state: BookDocumentReaderState,
    mode: BookDocumentReadingMode,
    jumpHistory: BookDocumentJumpHistory,
    onNavigate: (BookDocumentNavigationTarget) -> Unit,
    onReturn: () -> Unit,
) {
    val snapshot = seekState.snapshot ?: return
    if (snapshot.paged != (mode != BookDocumentReadingMode.SCROLL)) return
    var draft by remember(snapshot.section.key, snapshot.pagePositions, mode) { mutableStateOf<Float?>(null) }
    var inputVisible by remember(snapshot.section.key, snapshot.pagePositions, mode) { mutableStateOf(false) }
    val sections = state.loadedSections[snapshot.section.owner.id]?.sections.orEmpty()
    val sectionIndex = sections.indexOfFirst { it.key == snapshot.section.key }
    fun sectionTarget(index: Int) = sections.getOrNull(index)?.let {
        BookDocumentNavigationTarget(it.owner, BookLocator(it.document.document.resourceId))
    }
    val chapterWindow = state.readingOrder.window(snapshot.section.owner.id)
    val previous = sectionTarget(sectionIndex - 1) ?: chapterWindow?.previous?.let { BookDocumentNavigationTarget(it) }
    val next = sectionTarget(sectionIndex + 1) ?: chapterWindow?.next?.let { BookDocumentNavigationTarget(it) }
    val sectionTitle = if (sections.size > 1) {
        stringResource(R.string.book_navigation_section, sectionIndex + 1, sections.size)
    } else {
        stringResource(R.string.book_navigation_chapter_position)
    }
    val commit: (Float) -> Unit = { value ->
        seekState.beginSeek(value)
        draft = null
        inputVisible = false
        onNavigate(snapshot.targetAt(value))
    }
    Column(Modifier.padding(bottom = KatariTheme.spacing.s)) {
        if (jumpHistory.returnTarget != null) {
            ReaderReturnPositionBar(onReturn = onReturn, onDismiss = jumpHistory::dismiss)
        }
        draft?.let { value ->
            Surface(
                modifier = Modifier.padding(horizontal = KatariTheme.spacing.xl, vertical = KatariTheme.spacing.xs),
                container = SurfaceContainer.Chrome,
                shape = KatariTheme.shapes.medium,
            ) {
                Text(
                    text = snapshot.previewAt(value),
                    modifier = Modifier.padding(KatariTheme.spacing.m),
                    role = TextRole.Body,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 2,
                )
            }
        }
        Pill(
            text = sectionTitle,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = KatariTheme.spacing.xs),
            container = SurfaceContainer.Chrome,
        )
        ReaderPositionNavigator(
            type = if (mode == BookDocumentReadingMode.PAGED_RTL) {
                ReaderPageNavigatorType.HORIZONTAL_RTL
            } else {
                ReaderPageNavigatorType.HORIZONTAL_LTR
            },
            onNextSection = { next?.let(onNavigate) },
            nextSectionEnabled = next != null,
            onPreviousSection = { previous?.let(onNavigate) },
            previousSectionEnabled = previous != null,
            value = draft ?: seekState.pendingValue ?: snapshot.value,
            valueRange = snapshot.range,
            steps = if (snapshot.paged) (snapshot.pagePositions.size - 2).coerceAtLeast(0) else 0,
            formatValue = { if (snapshot.paged) it.roundToInt().toString() else "${it.roundToInt()}%" },
            endLabel = if (snapshot.paged) snapshot.pagePositions.size.toString() else "100%",
            onValueChange = { draft = it },
            onValueChangeFinished = commit,
            onValueChangeCancelled = { draft = null },
            previousSectionDescription = if (sectionIndex > 0) {
                stringResource(R.string.book_navigation_previous_section)
            } else {
                sharedStringResource(MR.strings.action_previous_chapter)
            },
            nextSectionDescription = if (sectionIndex < sections.lastIndex) {
                stringResource(R.string.book_navigation_next_section)
            } else {
                sharedStringResource(MR.strings.action_next_chapter)
            },
            showSinglePageLabel = true,
            hapticFeedbackEnabled = snapshot.paged,
            onPositionClick = { inputVisible = true },
        )
    }
    if (inputVisible) {
        BookDocumentPositionDialog(snapshot, onGo = commit, onDismiss = { inputVisible = false })
    }
}

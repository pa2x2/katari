package mihon.entry.interactions.book.document.reader.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
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
import mihon.presentation.kit.navigation.FloatingToolbar
import mihon.presentation.kit.surface.OverlayPanel
import mihon.presentation.kit.surface.Pill
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import kotlin.math.roundToInt
import tachiyomi.presentation.core.i18n.stringResource as sharedStringResource

/**
 * Manga-familiar controls with a local preview draft, committed only on release or explicit input. With
 * [leadingActions] (windows that aren't compact) the slider floats in one row with the reader's actions around it,
 * [leadingActions] before it and [trailingActions] after, at most a readable width wide and centred over the page, and
 * the section label follows the slider's thumb. Without them the slider spans the window above the caller's own bar.
 */
@Composable
internal fun BookDocumentSeekControls(
    seekState: BookDocumentSeekState,
    state: BookDocumentReaderState,
    mode: BookDocumentReadingMode,
    jumpHistory: BookDocumentJumpHistory,
    onNavigate: (BookDocumentNavigationTarget) -> Unit,
    onReturn: () -> Unit,
    leadingActions: (@Composable RowScope.() -> Unit)? = null,
    trailingActions: @Composable RowScope.() -> Unit = {},
) {
    val snapshot = seekState.snapshot?.takeIf { it.paged == (mode != BookDocumentReadingMode.SCROLL) }
    if (snapshot == null) {
        // The actions stay while there is no position to seek, such as before the chapter is laid out
        leadingActions?.let { leading ->
            ControlRow {
                leading()
                Spacer(Modifier.weight(1f))
                trailingActions()
            }
        }
        return
    }
    var draft by remember(snapshot.section.key, snapshot.pagePositions, mode) { mutableStateOf<Float?>(null) }
    var inputVisible by remember(snapshot.section.key, snapshot.pagePositions, mode) { mutableStateOf(false) }
    var track by remember { mutableStateOf<ClosedFloatingPointRange<Float>?>(null) }
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
    val value = draft ?: seekState.pendingValue ?: snapshot.value
    val rtl = mode == BookDocumentReadingMode.PAGED_RTL
    val navigator = @Composable { modifier: Modifier, framed: Boolean ->
        ReaderPositionNavigator(
            type = if (rtl) ReaderPageNavigatorType.HORIZONTAL_RTL else ReaderPageNavigatorType.HORIZONTAL_LTR,
            onNextSection = { next?.let(onNavigate) },
            nextSectionEnabled = next != null,
            onPreviousSection = { previous?.let(onNavigate) },
            previousSectionEnabled = previous != null,
            value = value,
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
            modifier = modifier,
            showSinglePageLabel = true,
            hapticFeedbackEnabled = snapshot.paged,
            onPositionClick = { inputVisible = true },
            framed = framed,
            leading = leadingActions ?: {},
            trailing = trailingActions,
            sliderModifier = Modifier.onPlaced { coordinates ->
                val left = coordinates.positionInRoot().x
                track = left..(left + coordinates.size.width)
            },
        )
    }
    val overlays = @Composable {
        if (jumpHistory.returnTarget != null) {
            ReaderReturnPositionBar(onReturn = onReturn, onDismiss = jumpHistory::dismiss)
        }
        draft?.let { value ->
            OverlayPanel(
                modifier = Modifier.padding(horizontal = KatariTheme.spacing.xl, vertical = KatariTheme.spacing.xs),
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
    }
    if (leadingActions == null) {
        Column(Modifier.padding(bottom = KatariTheme.spacing.s)) {
            overlays()
            Pill(
                text = sectionTitle,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = KatariTheme.spacing.xs),
                container = SurfaceContainer.Chrome,
            )
            navigator(Modifier, true)
        }
    } else {
        val range = snapshot.range
        val fraction = if (range.endInclusive > range.start) {
            ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
        } else {
            0f
        }
        ControlRow(
            above = {
                Column(
                    modifier = Modifier.widthIn(max = KatariTheme.sizes.readableWidth),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) { overlays() }
                ThumbLabel(sectionTitle, if (rtl) 1f - fraction else fraction, track)
            },
        ) {
            navigator(Modifier.weight(1f), false)
        }
    }
    if (inputVisible) {
        BookDocumentPositionDialog(snapshot, onGo = commit, onDismiss = { inputVisible = false })
    }
}

/**
 * The floating row of reader controls, [above] stacked over it: raised, at most a readable width wide and centred over
 * the page, clear of the navigation bar and a side cutout.
 */
@Composable
private fun ControlRow(above: @Composable ColumnScope.() -> Unit = {}, content: @Composable RowScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
            )
            .padding(bottom = KatariTheme.spacing.m),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        above()
        FloatingToolbar(modifier = Modifier.widthIn(max = KatariTheme.sizes.readableWidth), content = content)
    }
}

/** [text] centred over the slider's thumb, [fraction] of the way along [track] (window x), kept within the row. */
@Composable
private fun ThumbLabel(text: String, fraction: Float, track: ClosedFloatingPointRange<Float>?) {
    var left by remember { mutableFloatStateOf(0f) }
    Layout(
        content = {
            Pill(text = text, container = SurfaceContainer.Chrome)
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = KatariTheme.spacing.xs)
            .onPlaced { left = it.positionInRoot().x },
    ) { measurables, constraints ->
        val label = measurables.single().measure(constraints.copy(minWidth = 0))
        val centre = track?.let { it.start + (it.endInclusive - it.start) * fraction - left }
            ?: (constraints.maxWidth / 2f)
        val x = (centre - label.width / 2f).roundToInt()
            .coerceIn(0, (constraints.maxWidth - label.width).coerceAtLeast(0))
        layout(constraints.maxWidth, label.height) { label.place(x, 0) }
    }
}

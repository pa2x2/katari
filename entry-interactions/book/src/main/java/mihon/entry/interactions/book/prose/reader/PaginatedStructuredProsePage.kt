package mihon.entry.interactions.book.prose

import android.text.Layout
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import mihon.book.api.document.BookDocumentBlockContent
import mihon.book.api.document.BookDocumentPosition
import mihon.entry.interactions.book.document.reader.LocalBookDocumentSectionKey
import mihon.entry.interactions.book.document.render.PreparedBookDocument
import mihon.entry.interactions.book.document.render.PreparedBookDocumentBlock
import mihon.entry.interactions.settings.HtmlProseSettingsProvider
import kotlin.math.roundToInt

@Composable
internal fun PaginatedStructuredProsePage(
    index: Int,
    item: ProsePagerItem.Page,
    structuredBlock: PreparedBookDocumentBlock,
    state: HtmlProseReaderUiState,
    documents: Map<Long, PreparedBookDocument>,
    items: List<ProsePagerItem>,
    palette: ProsePalette,
    fontFamily: String,
    fontSizePercent: Int,
    lineHeightPercent: Int,
    textAlignment: String,
    pageModifier: Modifier,
    initialDocumentPosition: BookDocumentPosition?,
    initialPage: Int,
    laidOutPages: androidx.compose.runtime.snapshots.SnapshotStateMap<String, Boolean>,
    pagerState: androidx.compose.foundation.pager.PagerState,
    pendingAnchor: androidx.compose.runtime.MutableState<PendingBookDocumentAnchor?>,
    scope: kotlinx.coroutines.CoroutineScope,
    onAnchorClick: (String, TextView) -> Unit,
    onPosition: (ProseViewerPosition) -> Unit,
    onExternalLinkClick: (String) -> Unit,
) {
    val scrollState = rememberScrollState()
    val block = structuredBlock.block
    val restoredOffset = initialDocumentPosition
        ?.takeIf {
            index == initialPage &&
                item.page.chapter.id == state.currentChapterId &&
                it.blockId == block.id
        }
        ?.offsetWithinBlock
    var restorationComplete by remember(item.key, scrollState) {
        mutableStateOf(false)
    }
    var containerCoordinates by remember(item.key) {
        mutableStateOf<LayoutCoordinates?>(null)
    }
    var hasHiddenContent by remember(item.key) {
        mutableStateOf(
            (block.content as? BookDocumentBlockContent.Disclosure)
                ?.let { !it.initiallyExpanded }
                ?: false,
        )
    }
    LaunchedEffect(
        scrollState,
        restoredOffset,
        laidOutPages[item.key],
    ) {
        if (laidOutPages[item.key] != true) return@LaunchedEffect
        withFrameNanos {}
        restoredOffset?.let { offset ->
            scrollState.scrollTo(
                structuredBlockScrollValue(
                    offsetWithinBlock = offset,
                    blockLength = block.logicalLength,
                    maxScrollValue = scrollState.maxValue,
                ),
            )
        }
        restorationComplete = true
    }
    LaunchedEffect(
        scrollState,
        restorationComplete,
        pagerState,
        items,
    ) {
        if (!restorationComplete) return@LaunchedEffect
        snapshotFlow {
            if (
                pagerState.settledPage != index ||
                pagerState.isScrollInProgress
            ) {
                null
            } else {
                Triple(scrollState.value, scrollState.maxValue, !hasHiddenContent)
            }
        }
            .filter { it != null }
            .distinctUntilChanged()
            .collect { values ->
                values ?: return@collect
                val offset = structuredBlockPositionOffset(
                    blockLength = block.logicalLength,
                    scrollValue = values.first,
                    maxScrollValue = values.second,
                    contentFullyVisible = values.third,
                )
                val position = BookDocumentPosition(block.id, offset)
                val document = documents[item.page.chapter.id]?.document ?: return@collect
                onPosition(
                    ProseViewerPosition(
                        chapterId = item.page.chapter.id,
                        progression = document.progressionAt(position),
                        currentPage = item.page.index + 1,
                        totalPages = item.page.total,
                        documentPosition = position,
                    ),
                )
            }
    }
    val requestedAnchor = pendingAnchor.value?.takeIf {
        it.chapterId == item.page.chapter.id &&
            it.position.blockId == block.id
    }
    Box(
        modifier = pageModifier
            .verticalScroll(scrollState)
            .onGloballyPositioned { containerCoordinates = it },
        contentAlignment = Alignment.TopCenter,
    ) {
        CompositionLocalProvider(
            LocalBookDocumentSectionKey provides item.page.chapter.id.toString(),
        ) {
            ProseDocumentBlock(
                content = structuredBlock,
                resourceLoader = state.loadedChapters[item.page.chapter.id]?.resourceLoader,
                readerForeground = palette.foreground,
                readerBackground = palette.background,
                readerTypeface = proseTypeface(fontFamily),
                readerTextSizeSp = 16f * fontSizePercent / 100f,
                lineSpacingMultiplier = lineHeightPercent / 100f,
                readerTextAlignment = textAlignment.toTextViewAlignment(),
                justificationMode = if (
                    textAlignment == HtmlProseSettingsProvider.ALIGN_JUSTIFY
                ) {
                    Layout.JUSTIFICATION_MODE_INTER_WORD
                } else {
                    Layout.JUSTIFICATION_MODE_NONE
                },
                trimTerminalLine = false,
                onAnchorClick = onAnchorClick,
                onExternalLinkClick = onExternalLinkClick,
                onViewChanged = {},
                anchorOffsetWithinBlock = requestedAnchor?.position?.offsetWithinBlock,
                onAnchorTargetPositioned = { coordinates, offsetPx ->
                    val container = containerCoordinates
                        ?.takeIf { it.isAttached }
                        ?: return@ProseDocumentBlock
                    if (!coordinates.isAttached) return@ProseDocumentBlock
                    val target = (
                        scrollState.value +
                            coordinates.positionInWindow().y -
                            container.positionInWindow().y +
                            offsetPx
                        ).roundToInt()
                        .coerceIn(0, scrollState.maxValue)
                    pendingAnchor.value = null
                    scope.launch { scrollState.animateScrollTo(target) }
                },
                onHiddenContentChanged = { hasHiddenContent = it },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

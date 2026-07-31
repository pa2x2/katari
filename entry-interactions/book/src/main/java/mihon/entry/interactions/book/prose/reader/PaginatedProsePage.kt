package mihon.entry.interactions.book.prose

import android.text.Layout
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mihon.book.api.document.BookDocumentBlockContent
import mihon.book.api.document.BookDocumentPosition
import mihon.entry.interactions.book.document.reader.BookDocumentText
import mihon.entry.interactions.book.document.reader.LocalBookDocumentSectionKey
import mihon.entry.interactions.book.document.render.PreparedBookDocument
import mihon.entry.interactions.settings.HtmlProseSettingsProvider
import tachiyomi.domain.entry.model.EntryChapter

@Composable
internal fun PaginatedProseItem(
    index: Int,
    item: ProsePagerItem,
    documents: Map<Long, PreparedBookDocument>,
    pages: Map<Long, List<HtmlProsePage>>,
    items: List<ProsePagerItem>,
    state: HtmlProseReaderUiState,
    palette: ProsePalette,
    fontFamily: String,
    fontSizePercent: Int,
    lineHeightPercent: Int,
    textAlignment: String,
    horizontalMargin: Dp,
    verticalMargin: Dp,
    initialDocumentPosition: BookDocumentPosition?,
    initialPage: Int,
    laidOutPages: androidx.compose.runtime.snapshots.SnapshotStateMap<String, Boolean>,
    pagerState: androidx.compose.foundation.pager.PagerState,
    pendingAnchor: androidx.compose.runtime.MutableState<PendingBookDocumentAnchor?>,
    scope: kotlinx.coroutines.CoroutineScope,
    onPosition: (ProseViewerPosition) -> Unit,
    onExternalLinkClick: (String) -> Unit,
    onTransitionChapterRetry: (EntryChapter) -> Unit,
) {
    when (val item = items[index]) {
        is ProsePagerItem.Page -> {
            val onAnchorClick: (String, TextView) -> Unit = anchorClick@{ anchorId, _ ->
                val document = documents[item.page.chapter.id] ?: return@anchorClick
                val anchorPosition = document.document.anchors[anchorId]
                    ?: return@anchorClick
                val anchorOffset = document.document.logicalOffset(anchorPosition)
                    ?: return@anchorClick
                val targetPage = pageIndexForAnchor(
                    pages = pages[item.page.chapter.id].orEmpty(),
                    anchorOffset = anchorOffset,
                ) ?: return@anchorClick
                val targetIndex = items.indexOfFirst { target ->
                    target is ProsePagerItem.Page &&
                        target.page.chapter.id == item.page.chapter.id &&
                        target.page.index == targetPage
                }
                if (targetIndex >= 0) {
                    val targetBlock = document.block(anchorPosition.blockId)
                    pendingAnchor.value = targetBlock
                        ?.takeIf { it.block.content !is BookDocumentBlockContent.Text }
                        ?.let {
                            PendingBookDocumentAnchor(
                                chapterId = item.page.chapter.id,
                                position = anchorPosition,
                            )
                        }
                    scope.launch { pagerState.animateScrollToPage(targetIndex) }
                }
            }
            val pageModifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { laidOutPages[item.key] = true }
                .padding(horizontal = horizontalMargin, vertical = verticalMargin)
            val structuredBlock = item.page.structuredBlock
            if (structuredBlock == null) {
                CompositionLocalProvider(
                    LocalBookDocumentSectionKey provides item.page.chapter.id.toString(),
                ) {
                    BookDocumentText(
                        text = item.page.text,
                        documentTextIdentity = "page:${item.page.chapter.id}:${item.page.index}",
                        textColor = palette.foreground.toReaderArgb(),
                        textSizeSp = 16f * fontSizePercent / 100f,
                        typeface = proseTypeface(fontFamily),
                        lineSpacingMultiplier = lineHeightPercent / 100f,
                        textAlignment = textAlignment.toTextViewAlignment(),
                        justificationMode = if (
                            textAlignment == HtmlProseSettingsProvider.ALIGN_JUSTIFY
                        ) {
                            Layout.JUSTIFICATION_MODE_INTER_WORD
                        } else {
                            Layout.JUSTIFICATION_MODE_NONE
                        },
                        onAnchorClick = onAnchorClick,
                        onExternalLinkClick = onExternalLinkClick,
                        onViewChanged = {},
                        modifier = pageModifier,
                    )
                }
            } else {
                PaginatedStructuredProsePage(
                    index = index,
                    item = item,
                    structuredBlock = structuredBlock,
                    state = state,
                    documents = documents,
                    items = items,
                    palette = palette,
                    fontFamily = fontFamily,
                    fontSizePercent = fontSizePercent,
                    lineHeightPercent = lineHeightPercent,
                    textAlignment = textAlignment,
                    pageModifier = pageModifier,
                    initialDocumentPosition = initialDocumentPosition,
                    initialPage = initialPage,
                    laidOutPages = laidOutPages,
                    pagerState = pagerState,
                    pendingAnchor = pendingAnchor,
                    scope = scope,
                    onAnchorClick = onAnchorClick,
                    onPosition = onPosition,
                    onExternalLinkClick = onExternalLinkClick,
                )
            }
        }
        is ProsePagerItem.Transition -> ProseTransition(
            transition = item.transition,
            loadState = item.transition.to
                ?.let { state.transitionLoadStates[it.id] }
                .toSharedLoadState(),
            onRetry = item.transition.to?.let { chapter ->
                { onTransitionChapterRetry(chapter) }
            },
            palette = palette,
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
        )
    }
}

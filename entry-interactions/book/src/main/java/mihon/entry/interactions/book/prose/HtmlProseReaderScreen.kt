package mihon.entry.interactions.book.prose

import android.graphics.Typeface
import android.text.Layout
import android.text.TextPaint
import android.widget.TextView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ViewCarousel
import androidx.compose.material.icons.outlined.ViewStream
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mihon.entry.interactions.book.BookReaderNavigationRow
import mihon.entry.interactions.book.BookReaderNavigationSheet
import mihon.entry.interactions.book.R
import mihon.entry.interactions.settings.HtmlProseSettingsProvider
import mihon.entry.interactions.viewer.EntryChildDirection
import mihon.entry.interactions.viewer.EntryChildTransition
import mihon.entry.interactions.viewer.EntryChildWindow
import mihon.entry.viewer.settings.ResolvedViewerSetting
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.service.calculateChapterGap
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.AdaptiveSheet
import tachiyomi.presentation.core.components.CheckboxItem
import tachiyomi.presentation.core.components.HeadingItem
import tachiyomi.presentation.core.components.SettingsItemsPaddings
import tachiyomi.presentation.core.components.SliderItem
import tachiyomi.presentation.core.components.material.TabText
import tachiyomi.presentation.core.components.reader.ReaderChrome
import tachiyomi.presentation.core.components.reader.ReaderEntryChildTransition
import tachiyomi.presentation.core.components.reader.ReaderEntryChildTransitionItem
import tachiyomi.presentation.core.components.reader.ReaderEntryChildTransitionUiModel
import tachiyomi.presentation.core.components.reader.ReaderPageIndicator
import tachiyomi.presentation.core.components.reader.ReaderPageNavigator
import tachiyomi.presentation.core.components.reader.ReaderPageNavigatorType
import tachiyomi.presentation.core.components.reader.ReaderProgressIndicator
import tachiyomi.presentation.core.components.reader.ReaderProgressNavigator
import kotlin.math.roundToInt
import tachiyomi.presentation.core.i18n.stringResource as i18nStringResource

internal data class HtmlProseReaderUiState(
    val entryTitle: String,
    val currentChapterId: Long,
    val chapters: List<EntryChapter>,
    val window: EntryChildWindow<EntryChapter>,
    val loadedChapters: Map<Long, HtmlProseLoadedChapter>,
    val viewerResetKey: Long = 0,
    val menuVisible: Boolean = false,
    val chapterListVisible: Boolean = false,
    val settingsVisible: Boolean = false,
    val loadingChapterId: Long? = null,
    val loadError: String? = null,
)

@Composable
internal fun HtmlProseReaderScreen(
    state: HtmlProseReaderUiState,
    settings: HtmlProseSettingsBinding,
    onLocation: (chapterId: Long, progression: Float) -> Unit,
    onChapterEntered: (EntryChapter) -> Unit,
    onClose: () -> Unit,
    onMenuVisibilityChange: (Boolean) -> Unit,
    onChapterListVisibilityChange: (Boolean) -> Unit,
    onChapterSelected: (EntryChapter) -> Unit,
    onSettingsVisibilityChange: (Boolean) -> Unit,
) {
    val theme by settings.theme.state.collectEffectiveValue()
    val fontFamily by settings.fontFamily.state.collectEffectiveValue()
    val fontSize by settings.fontSize.state.collectEffectiveValue()
    val lineHeight by settings.lineHeight.state.collectEffectiveValue()
    val pageMargins by settings.pageMargins.state.collectEffectiveValue()
    val paragraphSpacing by settings.paragraphSpacing.state.collectEffectiveValue()
    val textAlignment by settings.textAlignment.state.collectEffectiveValue()
    val layoutMode by settings.layoutMode.state.collectEffectiveValue()
    val tapNavigation by settings.tapNavigation.state.collectEffectiveValue()
    val showProgress by settings.showProgress.state.collectEffectiveValue()
    val paginated = layoutMode == HtmlProseSettingsProvider.LAYOUT_PAGINATED
    val palette = prosePalette(theme, isSystemInDarkTheme())
    var position by remember(state.currentChapterId) {
        val progression = state.loadedChapters[state.currentChapterId]?.initialProgression ?: 0f
        mutableStateOf(ProseViewerPosition(state.currentChapterId, progression, 1, 1))
    }
    var viewerActions by remember { mutableStateOf(ProseViewerActions()) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = palette.background,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.displayCutout),
            ) {
                key(state.viewerResetKey) {
                    if (paginated) {
                        PaginatedProseViewer(
                            state = state,
                            palette = palette,
                            fontFamily = fontFamily,
                            fontSizePercent = fontSize,
                            lineHeightPercent = lineHeight,
                            pageMarginsPercent = pageMargins,
                            paragraphSpacingPercent = paragraphSpacing,
                            textAlignment = textAlignment,
                            tapNavigation = tapNavigation,
                            chromeVisible = state.menuVisible,
                            onPosition = {
                                position = it
                                onLocation(it.chapterId, it.progression)
                            },
                            onChapterEntered = onChapterEntered,
                            onMenuToggle = { onMenuVisibilityChange(!state.menuVisible) },
                            onActions = { viewerActions = it },
                        )
                    } else {
                        ScrollingProseViewer(
                            state = state,
                            initialProgression = position.progression,
                            palette = palette,
                            fontFamily = fontFamily,
                            fontSizePercent = fontSize,
                            lineHeightPercent = lineHeight,
                            pageMarginsPercent = pageMargins,
                            paragraphSpacingPercent = paragraphSpacing,
                            textAlignment = textAlignment,
                            onPosition = {
                                position = it
                                onLocation(it.chapterId, it.progression)
                            },
                            onChapterEntered = onChapterEntered,
                            onMenuToggle = { onMenuVisibilityChange(!state.menuVisible) },
                            onActions = { viewerActions = it },
                        )
                    }
                }
            }

            if (!state.menuVisible && showProgress) {
                val modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
                if (paginated) {
                    ReaderPageIndicator(position.currentPage, position.totalPages, modifier)
                } else {
                    ReaderProgressIndicator("${(position.progression * 100).toInt()}%", modifier)
                }
            }

            val chromeColor = MaterialTheme.colorScheme
                .surfaceColorAtElevation(3.dp)
                .copy(alpha = if (isSystemInDarkTheme()) 0.9f else 0.95f)
            ReaderChrome(
                visible = state.menuVisible,
                topBar = {
                    TopAppBar(
                        modifier = Modifier.background(chromeColor),
                        title = {
                            Column {
                                Text(state.entryTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(state.window.current.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onClose) {
                                Icon(
                                    Icons.AutoMirrored.Outlined.ArrowBack,
                                    stringResource(R.string.book_reader_close),
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = Color.Transparent,
                        ),
                    )
                },
                bottomBar = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (paginated) {
                            ReaderPageNavigator(
                                type = ReaderPageNavigatorType.HORIZONTAL_LTR,
                                onNextSection = viewerActions.nextSection,
                                nextSectionEnabled = state.window.next != null,
                                onPreviousSection = viewerActions.previousSection,
                                previousSectionEnabled = state.window.previous != null,
                                currentPage = position.currentPage,
                                totalPages = position.totalPages,
                                onPageIndexChange = viewerActions.seekPage,
                                showSinglePageLabel = true,
                                previousSectionDescription = stringResource(R.string.prose_reader_previous_chapter),
                                nextSectionDescription = stringResource(R.string.prose_reader_next_chapter),
                            )
                        } else {
                            ReaderProgressNavigator(
                                isRtl = false,
                                onNextSection = viewerActions.nextSection,
                                nextSectionEnabled = state.window.next != null,
                                onPreviousSection = viewerActions.previousSection,
                                previousSectionEnabled = state.window.previous != null,
                                currentProgress = position.progression,
                                onProgressChange = viewerActions.seekProgress,
                                onProgressChangeFinished = viewerActions.seekProgress,
                                previousSectionDescription = stringResource(R.string.prose_reader_previous_chapter),
                                nextSectionDescription = stringResource(R.string.prose_reader_next_chapter),
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(chromeColor)
                                .navigationBarsPadding()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            val scope = rememberCoroutineScope()
                            IconButton(
                                onClick = {
                                    val target = if (paginated) {
                                        HtmlProseSettingsProvider.LAYOUT_SCROLLING
                                    } else {
                                        HtmlProseSettingsProvider.LAYOUT_PAGINATED
                                    }
                                    scope.launch { settings.layoutMode.setEntryOverride(target) }
                                },
                            ) {
                                Icon(
                                    if (paginated) Icons.Outlined.ViewCarousel else Icons.Outlined.ViewStream,
                                    stringResource(R.string.prose_reader_layout),
                                )
                            }
                            IconButton(onClick = { onSettingsVisibilityChange(true) }) {
                                Icon(Icons.Outlined.Settings, stringResource(R.string.prose_reader_settings))
                            }
                            IconButton(onClick = { onChapterListVisibilityChange(true) }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ViewList,
                                    i18nStringResource(MR.strings.book_table_of_contents),
                                )
                            }
                        }
                    }
                },
            )

            state.loadError?.let { error ->
                Surface(
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    shape = MaterialTheme.shapes.large,
                    tonalElevation = 6.dp,
                ) {
                    Text(error, modifier = Modifier.padding(24.dp), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    BookReaderNavigationSheet(
        visible = state.chapterListVisible,
        rows = remember(state.chapters) {
            state.chapters.map { BookReaderNavigationRow(it, it.name) }
        },
        selectedIndex = state.chapters.indexOfFirst { it.id == state.currentChapterId },
        onItemClick = onChapterSelected,
        onDismissRequest = { onChapterListVisibilityChange(false) },
    )
    if (state.settingsVisible) {
        HtmlProseSettingsDialog(settings) { onSettingsVisibilityChange(false) }
    }
    BackHandler(enabled = state.chapterListVisible || state.settingsVisible) {
        if (state.chapterListVisible) onChapterListVisibilityChange(false) else onSettingsVisibilityChange(false)
    }
}

@Composable
private fun PaginatedProseViewer(
    state: HtmlProseReaderUiState,
    palette: ProsePalette,
    fontFamily: String,
    fontSizePercent: Int,
    lineHeightPercent: Int,
    pageMarginsPercent: Int,
    paragraphSpacingPercent: Int,
    textAlignment: String,
    tapNavigation: Boolean,
    chromeVisible: Boolean,
    onPosition: (ProseViewerPosition) -> Unit,
    onChapterEntered: (EntryChapter) -> Unit,
    onMenuToggle: () -> Unit,
    onActions: (ProseViewerActions) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val horizontalMargin = 20.dp * pageMarginsPercent / 100
        val verticalMargin = 16.dp * pageMarginsPercent / 100
        val widthPx = with(density) { (maxWidth - horizontalMargin * 2).roundToPx() }.coerceAtLeast(1)
        val heightPx = with(density) { (maxHeight - verticalMargin * 2).roundToPx() }.coerceAtLeast(1)
        val paint = remember(fontFamily, fontSizePercent, palette.foreground, density) {
            TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
                textSize = with(density) { (16.sp * fontSizePercent / 100).toPx() }
                color = palette.foreground.toArgbValue()
                typeface = proseTypeface(fontFamily)
            }
        }
        val alignment = textAlignment.toLayoutAlignment()
        val pages = remember(
            state.loadedChapters,
            widthPx,
            heightPx,
            paint.textSize,
            paint.typeface,
            alignment,
            lineHeightPercent,
            paragraphSpacingPercent,
        ) {
            state.loadedChapters.mapValues { (_, chapter) ->
                paginateProse(
                    chapter = chapter,
                    text = parseProseHtml(chapter.bodyHtml, paragraphSpacingPercent),
                    paint = paint,
                    availableWidthPx = widthPx,
                    availableHeightPx = heightPx,
                    alignment = alignment,
                    lineSpacingMultiplier = lineHeightPercent / 100f,
                    justificationMode = if (textAlignment == HtmlProseSettingsProvider.ALIGN_JUSTIFY) {
                        Layout.JUSTIFICATION_MODE_INTER_WORD
                    } else {
                        Layout.JUSTIFICATION_MODE_NONE
                    },
                )
            }
        }
        val items = remember(state.window, pages, state.loadingChapterId) {
            buildPaginatedItems(state.window, pages)
        }
        if (items.isEmpty()) return@BoxWithConstraints
        val initialPage = items.indexOfFirst { item ->
            item is ProsePagerItem.Page && item.page.chapter.id == state.currentChapterId &&
                item.page.progression >= (state.loadedChapters[state.currentChapterId]?.initialProgression ?: 0f)
        }.coerceAtLeast(0)
        val pagerState = rememberPagerState(initialPage = initialPage) { items.size }
        val scope = rememberCoroutineScope()
        LaunchedEffect(pagerState, items, state.currentChapterId) {
            onActions(
                ProseViewerActions(
                    seekPage = { pageIndex ->
                        items.indexOfFirst {
                            it is ProsePagerItem.Page &&
                                it.page.chapter.id == state.currentChapterId &&
                                it.page.index == pageIndex
                        }.takeIf { it >= 0 }?.let { scope.launch { pagerState.scrollToPage(it) } }
                    },
                    seekProgress = { progression ->
                        val chapterPages = items.filterIsInstance<ProsePagerItem.Page>()
                            .filter { it.page.chapter.id == state.currentChapterId }
                        val page = ((chapterPages.size - 1) * progression).roundToInt().coerceAtLeast(0)
                        chapterPages.getOrNull(page)?.let { target ->
                            items.indexOf(target).takeIf { it >= 0 }?.let {
                                scope.launch { pagerState.scrollToPage(it) }
                            }
                        }
                    },
                    previousSection = {
                        items.indexOfFirst {
                            it is ProsePagerItem.Transition &&
                                it.transition.direction == EntryChildDirection.PREVIOUS &&
                                it.transition.from.id == state.currentChapterId
                        }.takeIf { it >= 0 }?.let { scope.launch { pagerState.animateScrollToPage(it) } }
                    },
                    nextSection = {
                        items.indexOfFirst {
                            it is ProsePagerItem.Transition &&
                                it.transition.direction == EntryChildDirection.NEXT &&
                                it.transition.from.id == state.currentChapterId
                        }.takeIf { it >= 0 }?.let { scope.launch { pagerState.animateScrollToPage(it) } }
                    },
                ),
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(tapNavigation, chromeVisible, pagerState.currentPage) {
                    detectTapGestures { offset ->
                        val fraction = offset.x / size.width.coerceAtLeast(1)
                        when {
                            chromeVisible || !tapNavigation || fraction in 0.33f..0.66f -> onMenuToggle()
                            fraction < 0.33f && pagerState.currentPage > 0 -> scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                            fraction > 0.66f && pagerState.currentPage < items.lastIndex -> scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    }
                },
            key = { items[it].key },
            beyondViewportPageCount = 1,
        ) { index ->
            when (val item = items[index]) {
                is ProsePagerItem.Page -> ProseText(
                    text = item.page.text,
                    palette = palette,
                    fontFamily = fontFamily,
                    fontSizePercent = fontSizePercent,
                    lineHeightPercent = lineHeightPercent,
                    textAlignment = textAlignment,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = horizontalMargin, vertical = verticalMargin),
                )
                is ProsePagerItem.Transition -> ProseTransition(
                    transition = item.transition,
                    palette = palette,
                    modifier = Modifier.padding(28.dp),
                )
                is ProsePagerItem.Loading -> ProseLoading(
                    chapterName = item.chapter.name,
                    palette = palette,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        LaunchedEffect(pagerState, items) {
            snapshotFlow { pagerState.settledPage }
                .map { items.getOrNull(it) }
                .filter { it is ProsePagerItem.Page }
                .distinctUntilChanged()
                .collect { item ->
                    val page = (item as ProsePagerItem.Page).page
                    onPosition(ProseViewerPosition(page.chapter.id, page.progression, page.index + 1, page.total))
                    if (page.chapter.id != state.currentChapterId) onChapterEntered(page.chapter)
                }
        }
    }
}

@Composable
private fun ScrollingProseViewer(
    state: HtmlProseReaderUiState,
    initialProgression: Float,
    palette: ProsePalette,
    fontFamily: String,
    fontSizePercent: Int,
    lineHeightPercent: Int,
    pageMarginsPercent: Int,
    paragraphSpacingPercent: Int,
    textAlignment: String,
    onPosition: (ProseViewerPosition) -> Unit,
    onChapterEntered: (EntryChapter) -> Unit,
    onMenuToggle: () -> Unit,
    onActions: (ProseViewerActions) -> Unit,
) {
    val items = remember(state.window, state.loadedChapters) { buildScrollingItems(state.window, state.loadedChapters) }
    val initialIndex = items.indexOfFirst {
        it is ProseScrollItem.Chapter && it.content.chapter.id == state.currentChapterId
    }.coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    var initialPositionRestored by remember(listState, state.currentChapterId) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(listState, items, state.currentChapterId, initialProgression) {
        if (initialPositionRestored) return@LaunchedEffect
        val chapterIndex = items.indexOfFirst {
            it is ProseScrollItem.Chapter && it.content.chapter.id == state.currentChapterId
        }
        if (chapterIndex < 0) return@LaunchedEffect
        if (initialProgression > 0f) {
            val info = snapshotFlow {
                listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == chapterIndex }
            }.filter { it != null }.first() ?: return@LaunchedEffect
            listState.scrollToItem(
                chapterIndex,
                scrollOffsetForProgression(info.size, listState.layoutInfo.viewportSize.height, initialProgression),
            )
        }
        initialPositionRestored = true
    }
    LaunchedEffect(listState, items, state.currentChapterId) {
        val currentIndex = items.indexOfFirst {
            it is ProseScrollItem.Chapter && it.content.chapter.id == state.currentChapterId
        }
        onActions(
            ProseViewerActions(
                seekProgress = { progression ->
                    val info = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == currentIndex }
                    val scrollable = ((info?.size ?: 0) - listState.layoutInfo.viewportSize.height).coerceAtLeast(0)
                    if (currentIndex >= 0) {
                        scope.launch { listState.scrollToItem(currentIndex, (scrollable * progression).roundToInt()) }
                    }
                },
                previousSection = {
                    (currentIndex - 1).takeIf { it >= 0 }?.let { scope.launch { listState.animateScrollToItem(it) } }
                },
                nextSection = {
                    (currentIndex + 1).takeIf { it <= items.lastIndex }?.let {
                        scope.launch { listState.animateScrollToItem(it) }
                    }
                },
            ),
        )
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
    ) {
        items(items, key = ProseScrollItem::key) { item ->
            when (item) {
                is ProseScrollItem.Chapter -> ProseText(
                    text = parseProseHtml(item.content.bodyHtml, paragraphSpacingPercent),
                    palette = palette,
                    fontFamily = fontFamily,
                    fontSizePercent = fontSizePercent,
                    lineHeightPercent = lineHeightPercent,
                    textAlignment = textAlignment,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = null,
                            indication = null,
                            onClick = onMenuToggle,
                        )
                        .padding(
                            horizontal = 20.dp * pageMarginsPercent / 100,
                            vertical = 16.dp * pageMarginsPercent / 100,
                        ),
                )
                is ProseScrollItem.Transition -> ProseTransition(
                    transition = item.transition,
                    palette = palette,
                    modifier = Modifier
                        .fillParentMaxHeight()
                        .clickable(
                            interactionSource = null,
                            indication = null,
                            onClick = onMenuToggle,
                        )
                        .padding(28.dp),
                )
                is ProseScrollItem.Loading -> ProseLoading(
                    chapterName = item.chapter.name,
                    palette = palette,
                    modifier = Modifier
                        .fillParentMaxHeight()
                        .clickable(
                            interactionSource = null,
                            indication = null,
                            onClick = onMenuToggle,
                        ),
                )
            }
        }
    }
    LaunchedEffect(listState, items, initialPositionRestored) {
        if (!initialPositionRestored) return@LaunchedEffect
        snapshotFlow { listState.layoutInfo }
            .map { info ->
                val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
                info.visibleItemsInfo.minByOrNull { kotlin.math.abs((it.offset + it.size / 2) - center) }
            }
            .map { visible -> visible?.let { items.getOrNull(it.index) to it } }
            .filter { it?.first is ProseScrollItem.Chapter }
            .distinctUntilChanged { old, new ->
                old?.first?.key == new?.first?.key &&
                    old?.second?.offset == new?.second?.offset
            }
            .collect { pair ->
                val item = pair?.first as? ProseScrollItem.Chapter ?: return@collect
                val info = pair.second
                val viewport = listState.layoutInfo.viewportSize.height.coerceAtLeast(1)
                val scrollable = (info.size - viewport).coerceAtLeast(1)
                val progression = (-info.offset).toFloat().div(scrollable).coerceIn(0f, 1f)
                onPosition(ProseViewerPosition(item.content.chapter.id, progression, 1, 1))
                if (item.content.chapter.id != state.currentChapterId) onChapterEntered(item.content.chapter)
            }
    }
}

internal fun scrollOffsetForProgression(itemSize: Int, viewportSize: Int, progression: Float): Int {
    val scrollable = (itemSize - viewportSize).coerceAtLeast(0)
    return (scrollable * progression.coerceIn(0f, 1f)).roundToInt()
}

@Composable
private fun ProseText(
    text: CharSequence,
    palette: ProsePalette,
    fontFamily: String,
    fontSizePercent: Int,
    lineHeightPercent: Int,
    textAlignment: String,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextView(context).apply {
                includeFontPadding = false
                setTextIsSelectable(false)
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        },
        update = { view ->
            view.text = text
            view.setTextColor(palette.foreground.toArgbValue())
            view.textSize = 16f * fontSizePercent / 100f
            view.typeface = proseTypeface(fontFamily)
            view.setLineSpacing(0f, lineHeightPercent / 100f)
            view.textAlignment = textAlignment.toTextViewAlignment()
            view.justificationMode = if (textAlignment == HtmlProseSettingsProvider.ALIGN_JUSTIFY) {
                Layout.JUSTIFICATION_MODE_INTER_WORD
            } else {
                Layout.JUSTIFICATION_MODE_NONE
            }
        },
    )
}

@Composable
private fun ProseTransition(
    transition: EntryChildTransition<EntryChapter>,
    palette: ProsePalette,
    modifier: Modifier = Modifier,
) {
    val current = transition.from.toTransitionItem()
    val destination = transition.to?.toTransitionItem()
    val model = when (transition.direction) {
        EntryChildDirection.PREVIOUS -> ReaderEntryChildTransitionUiModel(
            topLabel = i18nStringResource(MR.strings.transition_previous),
            topChild = destination,
            bottomLabel = i18nStringResource(MR.strings.transition_current),
            bottomChild = current,
            fallbackLabel = i18nStringResource(MR.strings.transition_no_previous),
            missingChildCount = calculateChapterGap(
                transition.from.chapterNumber,
                transition.to?.chapterNumber ?: -1.0,
            ),
        )
        EntryChildDirection.NEXT -> ReaderEntryChildTransitionUiModel(
            topLabel = i18nStringResource(MR.strings.transition_finished),
            topChild = current,
            bottomLabel = i18nStringResource(MR.strings.transition_next),
            bottomChild = destination,
            fallbackLabel = i18nStringResource(MR.strings.transition_no_next),
            missingChildCount = calculateChapterGap(
                transition.to?.chapterNumber ?: -1.0,
                transition.from.chapterNumber,
            ),
        )
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        ReaderEntryChildTransition(
            model = model,
            contentColor = palette.foreground,
            accentColor = palette.foreground,
            warningColor = palette.foreground,
            outlineColor = palette.foreground.copy(alpha = 0.38f),
        )
    }
}

@Composable
private fun ProseLoading(
    chapterName: String,
    palette: ProsePalette,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = palette.foreground)
        Text(
            text = chapterName,
            modifier = Modifier.padding(top = 16.dp),
            color = palette.foreground,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun HtmlProseSettingsDialog(
    settings: HtmlProseSettingsBinding,
    onDismissRequest: () -> Unit,
) {
    val tabs = listOf(
        i18nStringResource(MR.strings.pref_category_display),
        i18nStringResource(MR.strings.pref_epub_page_layout),
        i18nStringResource(MR.strings.pref_epub_controls),
    )
    val pagerState = rememberPagerState { tabs.size }
    val scope = rememberCoroutineScope()
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = true),
    ) {
        BoxWithConstraints {
            AdaptiveSheet(
                isTabletUi = maxWidth >= 720.dp,
                enableImplicitDismiss = true,
                onDismissRequest = onDismissRequest,
                modifier = Modifier.heightIn(max = maxHeight * 0.75f),
            ) {
                Column {
                    PrimaryTabRow(
                        selectedTabIndex = pagerState.currentPage,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        divider = {},
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = pagerState.currentPage == index,
                                onClick = { scope.launch { pagerState.scrollToPage(index) } },
                                text = { TabText(title) },
                            )
                        }
                    }
                    HorizontalDivider()
                    HorizontalPager(state = pagerState) { page ->
                        Column(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 8.dp),
                        ) {
                            when (page) {
                                0 -> ProseAppearanceSettings(settings)
                                1 -> ProseLayoutSettings(settings)
                                2 -> ProseControlSettings(settings)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProseAppearanceSettings(settings: HtmlProseSettingsBinding) {
    val theme by settings.theme.state.collectEffectiveValue()
    val font by settings.fontFamily.state.collectEffectiveValue()
    val fontSize by settings.fontSize.state.collectEffectiveValue()
    ProseSettingChips(
        stringResource(R.string.prose_reader_theme),
        listOf(
            HtmlProseSettingsProvider.THEME_SYSTEM to stringResource(R.string.prose_reader_theme_system),
            HtmlProseSettingsProvider.THEME_LIGHT to stringResource(R.string.prose_reader_theme_light),
            HtmlProseSettingsProvider.THEME_SEPIA to stringResource(R.string.prose_reader_theme_sepia),
            HtmlProseSettingsProvider.THEME_DARK to stringResource(R.string.prose_reader_theme_dark),
            HtmlProseSettingsProvider.THEME_BLACK to stringResource(R.string.prose_reader_theme_black),
        ),
        theme,
        settings.theme::setProfileValue,
    )
    ProseSettingChips(
        stringResource(R.string.prose_reader_font),
        listOf(
            HtmlProseSettingsProvider.FONT_SERIF to stringResource(R.string.prose_reader_font_serif),
            HtmlProseSettingsProvider.FONT_SANS_SERIF to stringResource(R.string.prose_reader_font_sans_serif),
            HtmlProseSettingsProvider.FONT_MONOSPACE to stringResource(R.string.prose_reader_font_monospace),
        ),
        font,
        settings.fontFamily::setProfileValue,
    )
    SliderItem(
        value = fontSize,
        valueRange = HtmlProseSettingsProvider.FONT_SIZE_RANGE step 10,
        label = stringResource(R.string.prose_reader_font_size),
        valueString = "$fontSize%",
        onChange = settings.fontSize::setProfileValue,
    )
}

@Composable
private fun ProseLayoutSettings(settings: HtmlProseSettingsBinding) {
    val scope = rememberCoroutineScope()
    val layout by settings.layoutMode.state.collectEffectiveValue()
    val lineHeight by settings.lineHeight.state.collectEffectiveValue()
    val margins by settings.pageMargins.state.collectEffectiveValue()
    val paragraphSpacing by settings.paragraphSpacing.state.collectEffectiveValue()
    val alignment by settings.textAlignment.state.collectEffectiveValue()
    ProseSettingChips(
        stringResource(R.string.prose_reader_layout),
        listOf(
            HtmlProseSettingsProvider.LAYOUT_PAGINATED to stringResource(R.string.prose_reader_layout_paginated),
            HtmlProseSettingsProvider.LAYOUT_SCROLLING to stringResource(R.string.prose_reader_layout_scrolling),
        ),
        layout,
        { scope.launch { settings.layoutMode.setEntryOverride(it) } },
    )
    SliderItem(
        lineHeight,
        HtmlProseSettingsProvider.LINE_HEIGHT_RANGE step 10,
        stringResource(R.string.prose_reader_line_height),
        settings.lineHeight::setProfileValue,
        valueString = "$lineHeight%",
    )
    SliderItem(
        margins,
        HtmlProseSettingsProvider.PAGE_MARGINS_RANGE step 10,
        stringResource(R.string.prose_reader_page_margins),
        settings.pageMargins::setProfileValue,
        valueString = "$margins%",
    )
    SliderItem(
        paragraphSpacing,
        HtmlProseSettingsProvider.PARAGRAPH_SPACING_RANGE step 10,
        stringResource(R.string.prose_reader_paragraph_spacing),
        settings.paragraphSpacing::setProfileValue,
        valueString = "$paragraphSpacing%",
    )
    ProseSettingChips(
        stringResource(R.string.prose_reader_text_alignment),
        listOf(
            HtmlProseSettingsProvider.ALIGN_START to stringResource(R.string.prose_reader_alignment_start),
            HtmlProseSettingsProvider.ALIGN_JUSTIFY to stringResource(R.string.prose_reader_alignment_justify),
            HtmlProseSettingsProvider.ALIGN_LEFT to stringResource(R.string.prose_reader_alignment_left),
            HtmlProseSettingsProvider.ALIGN_RIGHT to stringResource(R.string.prose_reader_alignment_right),
        ),
        alignment,
        settings.textAlignment::setProfileValue,
    )
}

@Composable
private fun ProseControlSettings(settings: HtmlProseSettingsBinding) {
    val layout by settings.layoutMode.state.collectEffectiveValue()
    val tapNavigation by settings.tapNavigation.state.collectEffectiveValue()
    val showProgress by settings.showProgress.state.collectEffectiveValue()
    if (layout == HtmlProseSettingsProvider.LAYOUT_PAGINATED) {
        CheckboxItem(
            label = stringResource(R.string.prose_reader_tap_navigation),
            checked = tapNavigation,
            onClick = { settings.tapNavigation.setProfileValue(!tapNavigation) },
        )
    }
    CheckboxItem(
        label = stringResource(R.string.prose_reader_show_progress),
        checked = showProgress,
        onClick = { settings.showProgress.setProfileValue(!showProgress) },
    )
}

@Composable
private fun ProseSettingChips(
    label: String,
    values: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Column {
        HeadingItem(label)
        FlowRow(
            modifier = Modifier.padding(
                start = SettingsItemsPaddings.Horizontal,
                end = SettingsItemsPaddings.Horizontal,
                bottom = SettingsItemsPaddings.Vertical,
            ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            values.forEach { (value, text) ->
                FilterChip(selected == value, { onSelect(value) }, label = { Text(text) })
            }
        }
    }
}

internal fun buildPaginatedItems(
    window: EntryChildWindow<EntryChapter>,
    pages: Map<Long, List<HtmlProsePage>>,
): List<ProsePagerItem> = buildList {
    window.previous?.let { previous ->
        pages[previous.id]?.takeIf(List<*>::isNotEmpty)?.mapTo(this) { ProsePagerItem.Page(it) }
            ?: add(ProsePagerItem.Loading(previous))
    }
    add(ProsePagerItem.Transition(window.previousTransition()))
    pages[window.current.id]?.takeIf(List<*>::isNotEmpty)?.mapTo(this) { ProsePagerItem.Page(it) }
        ?: add(ProsePagerItem.Loading(window.current))
    add(ProsePagerItem.Transition(window.nextTransition()))
    window.next?.let { next ->
        pages[next.id]?.takeIf(List<*>::isNotEmpty)?.mapTo(this) { ProsePagerItem.Page(it) }
            ?: add(ProsePagerItem.Loading(next))
    }
}

internal fun buildScrollingItems(
    window: EntryChildWindow<EntryChapter>,
    loaded: Map<Long, HtmlProseLoadedChapter>,
): List<ProseScrollItem> = buildList {
    window.previous?.let { previous ->
        add(loaded[previous.id]?.let(ProseScrollItem::Chapter) ?: ProseScrollItem.Loading(previous))
    }
    add(ProseScrollItem.Transition(window.previousTransition()))
    add(loaded[window.current.id]?.let(ProseScrollItem::Chapter) ?: ProseScrollItem.Loading(window.current))
    add(ProseScrollItem.Transition(window.nextTransition()))
    window.next?.let { next ->
        add(loaded[next.id]?.let(ProseScrollItem::Chapter) ?: ProseScrollItem.Loading(next))
    }
}

internal sealed interface ProsePagerItem {
    val key: String

    data class Page(val page: HtmlProsePage) : ProsePagerItem {
        override val key = "page:${page.chapter.id}:${page.index}"
    }

    data class Transition(val transition: EntryChildTransition<EntryChapter>) : ProsePagerItem {
        override val key = "transition:${transitionKey(transition)}"
    }

    data class Loading(val chapter: EntryChapter) : ProsePagerItem {
        override val key = "loading:${chapter.id}"
    }
}

internal sealed interface ProseScrollItem {
    val key: String

    data class Chapter(val content: HtmlProseLoadedChapter) : ProseScrollItem {
        override val key = "chapter:${content.chapter.id}"
    }

    data class Transition(val transition: EntryChildTransition<EntryChapter>) : ProseScrollItem {
        override val key = "transition:${transitionKey(transition)}"
    }

    data class Loading(val chapter: EntryChapter) : ProseScrollItem {
        override val key = "loading:${chapter.id}"
    }
}

private data class ProseViewerPosition(
    val chapterId: Long,
    val progression: Float,
    val currentPage: Int,
    val totalPages: Int,
)

private data class ProseViewerActions(
    val seekPage: (Int) -> Unit = {},
    val seekProgress: (Float) -> Unit = {},
    val previousSection: () -> Unit = {},
    val nextSection: () -> Unit = {},
)

private data class ProsePalette(val background: Color, val foreground: Color)

@Composable
private fun prosePalette(theme: String, systemDark: Boolean): ProsePalette = when (theme) {
    HtmlProseSettingsProvider.THEME_LIGHT -> ProsePalette(Color(0xFFFAFAFA), Color(0xFF202124))
    HtmlProseSettingsProvider.THEME_DARK -> ProsePalette(Color(0xFF121212), Color(0xFFE6E1E5))
    HtmlProseSettingsProvider.THEME_SEPIA -> ProsePalette(Color(0xFFF4ECD8), Color(0xFF4B3A2A))
    HtmlProseSettingsProvider.THEME_BLACK -> ProsePalette(Color.Black, Color(0xFFE6E1E5))
    else -> if (systemDark) {
        ProsePalette(Color(0xFF121212), Color(0xFFE6E1E5))
    } else {
        ProsePalette(Color(0xFFFAFAFA), Color(0xFF202124))
    }
}

private fun proseTypeface(fontFamily: String): Typeface = when (fontFamily) {
    HtmlProseSettingsProvider.FONT_SANS_SERIF -> Typeface.SANS_SERIF
    HtmlProseSettingsProvider.FONT_MONOSPACE -> Typeface.MONOSPACE
    else -> Typeface.SERIF
}

private fun String.toLayoutAlignment(): Layout.Alignment = when (this) {
    HtmlProseSettingsProvider.ALIGN_RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
    HtmlProseSettingsProvider.ALIGN_LEFT -> Layout.Alignment.ALIGN_NORMAL
    else -> Layout.Alignment.ALIGN_NORMAL
}

private fun String.toTextViewAlignment(): Int = when (this) {
    HtmlProseSettingsProvider.ALIGN_LEFT -> TextView.TEXT_ALIGNMENT_TEXT_START
    HtmlProseSettingsProvider.ALIGN_RIGHT -> TextView.TEXT_ALIGNMENT_TEXT_END
    else -> TextView.TEXT_ALIGNMENT_VIEW_START
}

private fun Color.toArgbValue(): Int = android.graphics.Color.argb(
    (alpha * 255).toInt(),
    (red * 255).toInt(),
    (green * 255).toInt(),
    (blue * 255).toInt(),
)

private fun EntryChapter.toTransitionItem() = ReaderEntryChildTransitionItem(name, scanlator)

private fun transitionKey(transition: EntryChildTransition<EntryChapter>): String =
    "${transition.direction}:${transition.from.id}:${transition.to?.id ?: "terminal"}"

@Composable
private fun <T> StateFlow<ResolvedViewerSetting<T>>.collectEffectiveValue(): State<T> {
    val resolved by collectAsState()
    return rememberUpdatedState(resolved.effectiveValue)
}

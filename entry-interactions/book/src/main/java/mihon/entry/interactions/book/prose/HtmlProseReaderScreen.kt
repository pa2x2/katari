package mihon.entry.interactions.book.prose

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ViewCarousel
import androidx.compose.material.icons.outlined.ViewStream
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import mihon.entry.interactions.book.R
import mihon.entry.interactions.settings.HtmlProseSettingsProvider
import mihon.entry.interactions.viewer.EntryChildDirection
import mihon.entry.interactions.viewer.EntryChildTransition
import mihon.entry.viewer.settings.ResolvedViewerSetting
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.service.calculateChapterGap
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.reader.ReaderChrome
import tachiyomi.presentation.core.components.reader.ReaderEntryChildTransition
import tachiyomi.presentation.core.components.reader.ReaderEntryChildTransitionItem
import tachiyomi.presentation.core.components.reader.ReaderEntryChildTransitionUiModel
import tachiyomi.presentation.core.components.reader.ReaderPageIndicator
import tachiyomi.presentation.core.components.reader.ReaderPageNavigator
import tachiyomi.presentation.core.components.reader.ReaderPageNavigatorType
import tachiyomi.presentation.core.components.reader.ReaderProgressIndicator
import tachiyomi.presentation.core.components.reader.ReaderProgressNavigator
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt
import android.graphics.Color as AndroidColor
import tachiyomi.presentation.core.i18n.stringResource as i18nStringResource

internal data class HtmlProseReaderUiState(
    val entryTitle: String,
    val chapterTitle: String,
    val currentChapterId: Long,
    val resourceId: String,
    val bodyHtml: String,
    val progression: Float,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val menuVisible: Boolean = false,
    val settingsVisible: Boolean = false,
    val chapters: List<EntryChapter> = emptyList(),
    val chapterListVisible: Boolean = false,
    val transition: EntryChildTransition<EntryChapter>? = null,
    val transitionLoading: Boolean = false,
    val transitionError: String? = null,
)

@Composable
internal fun HtmlProseReaderScreen(
    state: HtmlProseReaderUiState,
    settings: HtmlProseSettingsBinding,
    onWebView: (ProseWebView) -> Unit,
    onLocation: (progression: Float, currentPage: Int, totalPages: Int) -> Unit,
    onTap: (horizontalFraction: Float) -> Unit,
    onBoundary: (EntryChildDirection) -> Unit,
    onClose: () -> Unit,
    onPreviousChapter: (() -> Unit)?,
    onNextChapter: (() -> Unit)?,
    onTransitionBack: () -> Unit,
    onTransitionContinue: () -> Unit,
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
    val showProgress by settings.showProgress.state.collectEffectiveValue()
    val paginated = layoutMode == HtmlProseSettingsProvider.LAYOUT_PAGINATED
    val palette = prosePalette(theme, isSystemInDarkTheme())
    var currentWebView by remember { mutableStateOf<ProseWebView?>(null) }
    val document = remember(
        state.resourceId,
        state.bodyHtml,
        paginated,
        palette,
        fontFamily,
        fontSize,
        lineHeight,
        pageMargins,
        paragraphSpacing,
        textAlignment,
    ) {
        buildReaderDocument(
            bodyHtml = state.bodyHtml,
            paginated = paginated,
            palette = palette,
            fontFamily = fontFamily,
            fontSizePercent = fontSize,
            lineHeightPercent = lineHeight,
            pageMarginsPercent = pageMargins,
            paragraphSpacingPercent = paragraphSpacing,
            textAlignment = textAlignment,
        )
    }

    Surface(modifier = Modifier.fillMaxSize(), color = palette.background) {
        Box(modifier = Modifier.fillMaxSize()) {
            key(document) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        createProseWebView(
                            context = context,
                            document = document,
                            paginated = paginated,
                            initialProgression = state.progression,
                            onLocation = onLocation,
                            onTap = onTap,
                            onBoundary = onBoundary,
                        ).also {
                            currentWebView = it
                            onWebView(it)
                        }
                    },
                    onRelease = { view ->
                        if (currentWebView === view) currentWebView = null
                        view.stopLoading()
                        view.loadUrl("about:blank")
                        view.clearHistory()
                        view.removeAllViews()
                        view.destroy()
                    },
                )
            }

            state.transition?.let { transition ->
                HtmlProseChapterTransition(
                    transition = transition,
                    loading = state.transitionLoading,
                    error = state.transitionError,
                    palette = palette,
                    paginated = paginated,
                    onBack = onTransitionBack,
                    onContinue = onTransitionContinue,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (state.transition == null && (state.transitionLoading || state.transitionError != null)) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = palette.background.copy(alpha = 0.96f),
                    contentColor = palette.foreground,
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        if (state.transitionLoading) {
                            CircularProgressIndicator()
                        } else {
                            Text(
                                text = state.transitionError.orEmpty(),
                                color = MaterialTheme.colorScheme.error,
                            )
                            TextButton(onClick = onTransitionBack) {
                                Text(stringResource(R.string.book_reader_close))
                            }
                        }
                    }
                }
            }

            if (
                state.transition == null &&
                !state.transitionLoading &&
                state.transitionError == null &&
                !state.menuVisible &&
                showProgress
            ) {
                val indicatorModifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                if (paginated) {
                    ReaderPageIndicator(
                        currentPage = state.currentPage,
                        totalPages = state.totalPages,
                        modifier = indicatorModifier,
                    )
                } else {
                    ReaderProgressIndicator(
                        text = "${(state.progression * 100).roundToInt()}%",
                        modifier = indicatorModifier,
                    )
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
                                Text(state.chapterTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onClose) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                    contentDescription = stringResource(R.string.book_reader_close),
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { onChapterListVisibilityChange(true) }) {
                                Icon(
                                    imageVector = Icons.Outlined.FormatListNumbered,
                                    contentDescription = stringResource(R.string.prose_reader_chapters),
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
                                onNextSection = { onNextChapter?.invoke() },
                                nextSectionEnabled = onNextChapter != null,
                                onPreviousSection = { onPreviousChapter?.invoke() },
                                previousSectionEnabled = onPreviousChapter != null,
                                currentPage = state.currentPage,
                                totalPages = state.totalPages,
                                onPageIndexChange = { index -> currentWebView?.seekPage(index) },
                                showSinglePageLabel = true,
                                previousSectionDescription = stringResource(R.string.prose_reader_previous_chapter),
                                nextSectionDescription = stringResource(R.string.prose_reader_next_chapter),
                            )
                        } else {
                            ReaderProgressNavigator(
                                isRtl = false,
                                onNextSection = { onNextChapter?.invoke() },
                                nextSectionEnabled = onNextChapter != null,
                                onPreviousSection = { onPreviousChapter?.invoke() },
                                previousSectionEnabled = onPreviousChapter != null,
                                currentProgress = state.progression,
                                onProgressChange = { progress -> currentWebView?.seekProgress(progress) },
                                onProgressChangeFinished = { progress -> currentWebView?.seekProgress(progress) },
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
                                    imageVector = if (paginated) {
                                        Icons.Outlined.ViewCarousel
                                    } else {
                                        Icons.Outlined.ViewStream
                                    },
                                    contentDescription = stringResource(R.string.prose_reader_layout),
                                )
                            }
                            IconButton(onClick = { onSettingsVisibilityChange(true) }) {
                                Icon(
                                    imageVector = Icons.Outlined.Settings,
                                    contentDescription = stringResource(R.string.prose_reader_settings),
                                )
                            }
                        }
                    }
                },
            )
        }
    }

    if (state.settingsVisible) {
        HtmlProseSettingsDialog(
            settings = settings,
            paginated = paginated,
            onDismissRequest = { onSettingsVisibilityChange(false) },
        )
    }
    if (state.chapterListVisible) {
        HtmlProseChapterList(
            chapters = state.chapters,
            currentChapterId = state.currentChapterId,
            onDismissRequest = { onChapterListVisibilityChange(false) },
            onChapterSelected = onChapterSelected,
        )
    }
}

@Composable
private fun HtmlProseChapterTransition(
    transition: EntryChildTransition<EntryChapter>,
    loading: Boolean,
    error: String?,
    palette: ProsePalette,
    paginated: Boolean,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentItem = transition.from.toTransitionItem()
    val destinationItem = transition.to?.toTransitionItem()
    val model = when (transition.direction) {
        EntryChildDirection.PREVIOUS -> ReaderEntryChildTransitionUiModel(
            topLabel = i18nStringResource(MR.strings.transition_previous),
            topChild = destinationItem,
            bottomLabel = i18nStringResource(MR.strings.transition_current),
            bottomChild = currentItem,
            fallbackLabel = i18nStringResource(MR.strings.transition_no_previous),
            missingChildCount = calculateChapterGap(
                transition.from.chapterNumber,
                transition.to?.chapterNumber ?: -1.0,
            ),
        )
        EntryChildDirection.NEXT -> ReaderEntryChildTransitionUiModel(
            topLabel = i18nStringResource(MR.strings.transition_finished),
            topChild = currentItem,
            bottomLabel = i18nStringResource(MR.strings.transition_next),
            bottomChild = destinationItem,
            fallbackLabel = i18nStringResource(MR.strings.transition_no_next),
            missingChildCount = calculateChapterGap(
                transition.to?.chapterNumber ?: -1.0,
                transition.from.chapterNumber,
            ),
        )
    }
    Surface(modifier = modifier, color = palette.background, contentColor = palette.foreground) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                ReaderEntryChildTransition(model)
                when {
                    loading -> CircularProgressIndicator()
                    error != null -> Text(error, color = MaterialTheme.colorScheme.error)
                    transition.to != null -> Button(onClick = onContinue) {
                        Text(stringResource(R.string.prose_reader_continue_chapter))
                    }
                }
                Text(
                    text = stringResource(R.string.prose_reader_transition_hint),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(transition, paginated) {
                        var distance = 0f
                        detectDragGestures(
                            onDragStart = { distance = 0f },
                            onDragEnd = {
                                if (kotlin.math.abs(distance) >= TRANSITION_SWIPE_DISTANCE_PX) {
                                    val gestureDirection = if (distance < 0f) {
                                        EntryChildDirection.NEXT
                                    } else {
                                        EntryChildDirection.PREVIOUS
                                    }
                                    if (gestureDirection == transition.direction) onContinue() else onBack()
                                }
                            },
                        ) { change, dragAmount ->
                            distance += if (paginated) dragAmount.x else dragAmount.y
                            change.consume()
                        }
                    },
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clickable {
                            if (transition.direction == EntryChildDirection.PREVIOUS) onContinue() else onBack()
                        },
                )
                Box(modifier = Modifier.weight(1f).fillMaxSize())
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clickable {
                            if (transition.direction == EntryChildDirection.NEXT) onContinue() else onBack()
                        },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HtmlProseChapterList(
    chapters: List<EntryChapter>,
    currentChapterId: Long,
    onDismissRequest: () -> Unit,
    onChapterSelected: (EntryChapter) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismissRequest) {
        Text(
            text = stringResource(R.string.prose_reader_chapters),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            style = MaterialTheme.typography.titleLarge,
        )
        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp)) {
            items(chapters, key = EntryChapter::id) { chapter ->
                ListItem(
                    headlineContent = { Text(chapter.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                    supportingContent = chapter.scanlator?.let { scanlator ->
                        { Text(scanlator, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    },
                    trailingContent = if (chapter.id == currentChapterId) {
                        {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = stringResource(R.string.prose_reader_current_chapter),
                            )
                        }
                    } else {
                        null
                    },
                    modifier = Modifier.clickable { onChapterSelected(chapter) },
                )
            }
        }
    }
}

private fun EntryChapter.toTransitionItem() = ReaderEntryChildTransitionItem(
    name = name,
    subtitle = scanlator,
)

@Composable
private fun HtmlProseSettingsDialog(
    settings: HtmlProseSettingsBinding,
    paginated: Boolean,
    onDismissRequest: () -> Unit,
) {
    val scope = rememberCoroutineScope()
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

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 680.dp)
                .padding(24.dp),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.prose_reader_settings), style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = onDismissRequest) {
                        Text(stringResource(R.string.book_reader_close))
                    }
                }
                ProseSettingChips(
                    label = stringResource(R.string.prose_reader_layout),
                    values = listOf(
                        HtmlProseSettingsProvider.LAYOUT_PAGINATED to
                            stringResource(R.string.prose_reader_layout_paginated),
                        HtmlProseSettingsProvider.LAYOUT_SCROLLING to
                            stringResource(R.string.prose_reader_layout_scrolling),
                    ),
                    selected = layoutMode,
                    onSelect = { scope.launch { settings.layoutMode.setEntryOverride(it) } },
                )
                ProseSettingChips(
                    label = stringResource(R.string.prose_reader_theme),
                    values = listOf(
                        HtmlProseSettingsProvider.THEME_SYSTEM to stringResource(R.string.prose_reader_theme_system),
                        HtmlProseSettingsProvider.THEME_LIGHT to stringResource(R.string.prose_reader_theme_light),
                        HtmlProseSettingsProvider.THEME_SEPIA to stringResource(R.string.prose_reader_theme_sepia),
                        HtmlProseSettingsProvider.THEME_DARK to stringResource(R.string.prose_reader_theme_dark),
                        HtmlProseSettingsProvider.THEME_BLACK to stringResource(R.string.prose_reader_theme_black),
                    ),
                    selected = theme,
                    onSelect = settings.theme::setProfileValue,
                )
                ProseSettingChips(
                    label = stringResource(R.string.prose_reader_font),
                    values = listOf(
                        HtmlProseSettingsProvider.FONT_SERIF to stringResource(R.string.prose_reader_font_serif),
                        HtmlProseSettingsProvider.FONT_SANS_SERIF to
                            stringResource(R.string.prose_reader_font_sans_serif),
                        HtmlProseSettingsProvider.FONT_MONOSPACE to
                            stringResource(R.string.prose_reader_font_monospace),
                    ),
                    selected = fontFamily,
                    onSelect = settings.fontFamily::setProfileValue,
                )
                ProseSlider(
                    label = stringResource(R.string.prose_reader_font_size),
                    value = fontSize,
                    range = HtmlProseSettingsProvider.FONT_SIZE_RANGE,
                    onChange = settings.fontSize::setProfileValue,
                )
                ProseSlider(
                    label = stringResource(R.string.prose_reader_line_height),
                    value = lineHeight,
                    range = HtmlProseSettingsProvider.LINE_HEIGHT_RANGE,
                    onChange = settings.lineHeight::setProfileValue,
                )
                ProseSlider(
                    label = stringResource(R.string.prose_reader_page_margins),
                    value = pageMargins,
                    range = HtmlProseSettingsProvider.PAGE_MARGINS_RANGE,
                    onChange = settings.pageMargins::setProfileValue,
                )
                ProseSlider(
                    label = stringResource(R.string.prose_reader_paragraph_spacing),
                    value = paragraphSpacing,
                    range = HtmlProseSettingsProvider.PARAGRAPH_SPACING_RANGE,
                    onChange = settings.paragraphSpacing::setProfileValue,
                )
                ProseSettingChips(
                    label = stringResource(R.string.prose_reader_text_alignment),
                    values = listOf(
                        HtmlProseSettingsProvider.ALIGN_START to stringResource(R.string.prose_reader_alignment_start),
                        HtmlProseSettingsProvider.ALIGN_JUSTIFY to
                            stringResource(R.string.prose_reader_alignment_justify),
                        HtmlProseSettingsProvider.ALIGN_LEFT to stringResource(R.string.prose_reader_alignment_left),
                        HtmlProseSettingsProvider.ALIGN_RIGHT to stringResource(R.string.prose_reader_alignment_right),
                    ),
                    selected = textAlignment,
                    onSelect = settings.textAlignment::setProfileValue,
                )
                if (paginated) {
                    ProseCheckbox(
                        label = stringResource(R.string.prose_reader_tap_navigation),
                        checked = tapNavigation,
                        onClick = { settings.tapNavigation.setProfileValue(!tapNavigation) },
                    )
                }
                ProseCheckbox(
                    label = stringResource(R.string.prose_reader_show_progress),
                    checked = showProgress,
                    onClick = { settings.showProgress.setProfileValue(!showProgress) },
                )
            }
        }
    }
}

@Composable
private fun ProseSettingChips(
    label: String,
    values: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            values.forEach { (value, text) ->
                FilterChip(selected = selected == value, onClick = { onSelect(value) }, label = { Text(text) })
            }
        }
    }
}

@Composable
private fun ProseSlider(
    label: String,
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit,
) {
    Column {
        Text("$label · $value%", style = MaterialTheme.typography.titleSmall)
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
        )
    }
}

@Composable
private fun ProseCheckbox(label: String, checked: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = { onClick() })
        Text(label)
    }
}

internal data class ProsePalette(val background: Color, val foreground: Color, val link: Color)

@Composable
private fun prosePalette(theme: String, systemDark: Boolean): ProsePalette = when (theme) {
    HtmlProseSettingsProvider.THEME_LIGHT -> ProsePalette(Color(0xFFFAFAFA), Color(0xFF202124), Color(0xFF315DA8))
    HtmlProseSettingsProvider.THEME_DARK -> ProsePalette(Color(0xFF121212), Color(0xFFE6E1E5), Color(0xFFAEC6FF))
    HtmlProseSettingsProvider.THEME_SEPIA -> ProsePalette(Color(0xFFF4ECD8), Color(0xFF4B3A2A), Color(0xFF765A2A))
    HtmlProseSettingsProvider.THEME_BLACK -> ProsePalette(Color.Black, Color(0xFFE6E1E5), Color(0xFFAEC6FF))
    else -> if (systemDark) {
        ProsePalette(Color(0xFF121212), Color(0xFFE6E1E5), Color(0xFFAEC6FF))
    } else {
        ProsePalette(Color(0xFFFAFAFA), Color(0xFF202124), Color(0xFF315DA8))
    }
}

internal fun buildReaderDocument(
    bodyHtml: String,
    paginated: Boolean,
    palette: ProsePalette,
    fontFamily: String,
    fontSizePercent: Int,
    lineHeightPercent: Int,
    pageMarginsPercent: Int,
    paragraphSpacingPercent: Int,
    textAlignment: String,
): String {
    val font = when (fontFamily) {
        HtmlProseSettingsProvider.FONT_SANS_SERIF -> "sans-serif"
        HtmlProseSettingsProvider.FONT_MONOSPACE -> "monospace"
        else -> "serif"
    }
    val alignment = when (textAlignment) {
        HtmlProseSettingsProvider.ALIGN_JUSTIFY -> "justify"
        HtmlProseSettingsProvider.ALIGN_LEFT -> "left"
        HtmlProseSettingsProvider.ALIGN_RIGHT -> "right"
        else -> "start"
    }
    val horizontalPadding = 1.25 * pageMarginsPercent / 100.0
    val verticalPadding = 1.0 * pageMarginsPercent / 100.0
    val layoutCss = if (paginated) {
        """
        html { height: 100%; overflow: hidden; }
        body {
          height: calc(100vh - ${2 * verticalPadding}rem);
          margin: ${verticalPadding}rem ${horizontalPadding}rem;
          padding: 0;
          column-width: calc(100vw - ${2 * horizontalPadding}rem);
          column-gap: ${2 * horizontalPadding}rem;
          column-fill: auto;
          overflow: visible;
        }
        """.trimIndent()
    } else {
        "body { max-width: 46rem; margin: 0 auto; padding: ${verticalPadding}rem ${horizontalPadding}rem 4rem; }"
    }
    return """
        <!doctype html>
        <html>
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1">
          <style>
            :root { color-scheme: light dark; }
            html { background: ${palette.background.cssColor()}; color: ${palette.foreground.cssColor()}; }
            $layoutCss
            body {
              box-sizing: border-box;
              background: ${palette.background.cssColor()};
              color: ${palette.foreground.cssColor()};
              font-family: $font;
              font-size: ${fontSizePercent / 100.0}rem;
              line-height: ${lineHeightPercent / 100.0};
              text-align: $alignment;
              overflow-wrap: anywhere;
            }
            p { margin-block: 0 ${paragraphSpacingPercent / 100.0}em; }
            h1, h2, h3, h4, h5, h6 { line-height: 1.25; break-after: avoid; }
            a { color: ${palette.link.cssColor()}; }
            blockquote { margin-inline: 0; padding-inline-start: 1rem; border-inline-start: 0.2rem solid currentColor; opacity: 0.85; }
            pre, code { white-space: pre-wrap; overflow-wrap: anywhere; }
            table { display: block; max-width: 100%; overflow-x: auto; border-collapse: collapse; }
            th, td { padding: 0.35rem 0.5rem; border: 1px solid currentColor; }
          </style>
        </head>
        <body>$bodyHtml</body>
        </html>
    """.trimIndent()
}

private fun Color.cssColor(): String = String.format(
    Locale.US,
    "#%02X%02X%02X",
    (red * 255).roundToInt(),
    (green * 255).roundToInt(),
    (blue * 255).roundToInt(),
)

internal class ProseWebView(context: Context) : WebView(context) {
    var paginated: Boolean = true
    var onMetricsChanged: ((Float, Int, Int) -> Unit)? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w != oldw || h != oldh) post(::dispatchMetrics)
    }

    fun movePage(delta: Int): Boolean {
        if (!paginated || width <= 0) return false
        val target = (currentPageIndex() + delta).coerceIn(0, totalPages() - 1)
        if (target == currentPageIndex()) return false
        scrollTo(target * width, 0)
        post(::dispatchMetrics)
        return true
    }

    fun seekPage(index: Int) {
        if (!paginated || width <= 0) return
        scrollTo(index.coerceIn(0, totalPages() - 1) * width, 0)
        dispatchMetrics()
    }

    fun seekProgress(progress: Float) {
        val safe = progress.coerceIn(0f, 1f)
        if (paginated) {
            seekPage((safe * (totalPages() - 1)).roundToInt())
        } else {
            scrollTo(0, (safe * verticalScrollRange()).roundToInt())
            dispatchMetrics()
        }
    }

    fun isAtBoundary(direction: EntryChildDirection): Boolean = when (direction) {
        EntryChildDirection.PREVIOUS -> if (paginated) currentPageIndex() == 0 else scrollY <= 0
        EntryChildDirection.NEXT -> if (paginated) {
            currentPageIndex() >= totalPages() - 1
        } else {
            scrollY >= verticalScrollRange()
        }
    }

    fun dispatchMetrics() {
        if (width <= 0 || height <= 0) return
        val total = if (paginated) totalPages() else 1
        val current = if (paginated) currentPageIndex() + 1 else 1
        val progression = if (paginated) {
            if (total <= 1) 1f else (current - 1).toFloat() / (total - 1)
        } else {
            val range = verticalScrollRange()
            if (range <= 0) 1f else scrollY.toFloat().div(range).coerceIn(0f, 1f)
        }
        onMetricsChanged?.invoke(progression, current, total)
    }

    private fun totalPages(): Int {
        if (width <= 0) return 1
        return ceil(computeHorizontalScrollRange().toDouble() / width).toInt().coerceAtLeast(1)
    }

    private fun currentPageIndex(): Int = if (width <= 0) 0 else (scrollX.toFloat() / width).roundToInt()

    private fun verticalScrollRange(): Int = (computeVerticalScrollRange() - height).coerceAtLeast(0)
}

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
internal fun createProseWebView(
    context: Context,
    document: String,
    paginated: Boolean,
    initialProgression: Float,
    onLocation: (Float, Int, Int) -> Unit,
    onTap: (Float) -> Unit,
    onBoundary: (EntryChildDirection) -> Unit,
): ProseWebView = ProseWebView(context).apply {
    this.paginated = paginated
    onMetricsChanged = onLocation
    setBackgroundColor(AndroidColor.TRANSPARENT)
    isHorizontalScrollBarEnabled = false
    isVerticalScrollBarEnabled = false
    overScrollMode = View.OVER_SCROLL_NEVER
    settings.apply {
        javaScriptEnabled = false
        domStorageEnabled = false
        databaseEnabled = false
        allowFileAccess = false
        allowContentAccess = false
        blockNetworkLoads = true
        loadsImagesAutomatically = false
        setSupportZoom(false)
        builtInZoomControls = false
        displayZoomControls = false
    }
    webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
            !request.url.isReaderAnchor()

        @Deprecated("Deprecated in Android")
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean =
            !Uri.parse(url).isReaderAnchor()

        override fun onPageFinished(view: WebView, url: String) {
            post {
                seekProgress(initialProgression)
                dispatchMetrics()
            }
        }
    }
    val detector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(event: MotionEvent): Boolean = true

            override fun onSingleTapUp(event: MotionEvent): Boolean {
                onTap(event.x / width.coerceAtLeast(1).toFloat())
                return true
            }

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float,
            ): Boolean {
                if (e1 == null) return false
                if (paginated) {
                    if (kotlin.math.abs(velocityX) <= kotlin.math.abs(velocityY)) return false
                    val direction = if (e2.x < e1.x) EntryChildDirection.NEXT else EntryChildDirection.PREVIOUS
                    if (!movePage(if (direction == EntryChildDirection.NEXT) 1 else -1)) onBoundary(direction)
                    return true
                }
                if (kotlin.math.abs(velocityY) <= kotlin.math.abs(velocityX)) return false
                val direction = if (e2.y < e1.y) EntryChildDirection.NEXT else EntryChildDirection.PREVIOUS
                val atBoundary = isAtBoundary(direction)
                if (atBoundary) onBoundary(direction)
                return atBoundary
            }
        },
    )
    setOnTouchListener { _, event ->
        val handled = detector.onTouchEvent(event)
        paginated && handled
    }
    setOnScrollChangeListener { _, _, _, _, _ -> dispatchMetrics() }
    loadDataWithBaseURL(READER_BASE_URL, document, HTML_MEDIA_TYPE, "utf-8", null)
}

private fun Uri.isReaderAnchor(): Boolean = scheme == "https" && host == READER_HOST && fragment != null

@Composable
private fun <T> kotlinx.coroutines.flow.StateFlow<ResolvedViewerSetting<T>>.collectEffectiveValue(): State<T> {
    val resolved by collectAsState()
    return rememberUpdatedState(resolved.effectiveValue)
}

private const val READER_HOST = "katari.invalid"
private const val READER_BASE_URL = "https://$READER_HOST/"
private const val TRANSITION_SWIPE_DISTANCE_PX = 72f

package mihon.entry.interactions.book.epub

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ViewCarousel
import androidx.compose.material.icons.outlined.ViewStream
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mihon.book.api.BookLocator
import mihon.book.api.BookNavigationItem
import mihon.book.api.BookReadingDirection
import mihon.entry.interactions.settings.ReadiumEpubSettingsProvider
import mihon.entry.viewer.settings.ResolvedViewerSetting
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.AdaptiveSheet
import tachiyomi.presentation.core.components.CheckboxItem
import tachiyomi.presentation.core.components.HeadingItem
import tachiyomi.presentation.core.components.SettingsItemsPaddings
import tachiyomi.presentation.core.components.SliderItem
import tachiyomi.presentation.core.components.material.TabText
import tachiyomi.presentation.core.components.reader.ReaderChrome
import tachiyomi.presentation.core.components.reader.ReaderPageIndicator
import tachiyomi.presentation.core.components.reader.ReaderPageNavigator
import tachiyomi.presentation.core.components.reader.ReaderPageNavigatorType
import tachiyomi.presentation.core.components.reader.ReaderProgressIndicator
import tachiyomi.presentation.core.components.reader.ReaderProgressNavigator
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.math.roundToInt

internal data class ReadiumEpubReaderUiState(
    val bookTitle: String,
    val sectionTitle: String? = null,
    val currentLocator: BookLocator? = null,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val currentSectionIndex: Int = -1,
    val sectionCount: Int = 0,
    val sectionProgress: Float = 0f,
    val readingDirection: BookReadingDirection? = null,
    val fixedLayout: Boolean = false,
    val menuVisible: Boolean = false,
    val tocVisible: Boolean = false,
    val settingsVisible: Boolean = false,
)

@Composable
internal fun ReadiumEpubReaderScreen(
    state: ReadiumEpubReaderUiState,
    navigation: List<ReadiumNavigationRow>,
    settings: ReadiumEpubSettingsBinding,
    onClose: () -> Unit,
    onTocVisibilityChange: (Boolean) -> Unit,
    onSettingsVisibilityChange: (Boolean) -> Unit,
    onPageIndexPreview: (Int) -> Unit,
    onPageIndexChange: (Int) -> Unit,
    onProgressPreview: (Float) -> Unit,
    onProgressChange: (Float) -> Unit,
    onPreviousSection: () -> Unit,
    onNextSection: () -> Unit,
    onNavigationItemClick: (BookNavigationItem) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val showPageNumber by settings.showPageNumber.state.collectEffectiveValue()
    val layoutMode by settings.layoutMode.state.collectEffectiveValue()
    val paginated = state.fixedLayout || layoutMode == ReadiumEpubSettingsProvider.LAYOUT_PAGINATED

    Box(modifier = Modifier.fillMaxSize()) {
        if (!state.menuVisible && showPageNumber) {
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
                    text = "${(state.sectionProgress * 100).roundToInt()}%",
                    modifier = indicatorModifier,
                )
            }
        }

        val backgroundColor = MaterialTheme.colorScheme
            .surfaceColorAtElevation(3.dp)
            .copy(alpha = if (isSystemInDarkTheme()) 0.9f else 0.95f)
        ReaderChrome(
            visible = state.menuVisible,
            topBar = {
                ReadiumReaderTopBar(
                    bookTitle = state.bookTitle,
                    sectionTitle = state.sectionTitle,
                    onClose = onClose,
                    modifier = Modifier.background(backgroundColor),
                )
            },
            bottomBar = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (paginated) {
                        ReaderPageNavigator(
                            type = if (state.readingDirection == BookReadingDirection.RIGHT_TO_LEFT) {
                                ReaderPageNavigatorType.HORIZONTAL_RTL
                            } else {
                                ReaderPageNavigatorType.HORIZONTAL_LTR
                            },
                            onNextSection = onNextSection,
                            nextSectionEnabled = state.currentSectionIndex in 0 until state.sectionCount - 1,
                            onPreviousSection = onPreviousSection,
                            previousSectionEnabled = state.currentSectionIndex > 0,
                            currentPage = state.currentPage,
                            totalPages = state.totalPages,
                            onPageIndexChange = onPageIndexPreview,
                            onPageIndexChangeFinished = onPageIndexChange,
                            previousSectionDescription = stringResource(MR.strings.action_previous_section),
                            nextSectionDescription = stringResource(MR.strings.action_next_section),
                        )
                    } else {
                        ReaderProgressNavigator(
                            isRtl = state.readingDirection == BookReadingDirection.RIGHT_TO_LEFT,
                            onNextSection = onNextSection,
                            nextSectionEnabled = state.currentSectionIndex in 0 until state.sectionCount - 1,
                            onPreviousSection = onPreviousSection,
                            previousSectionEnabled = state.currentSectionIndex > 0,
                            currentProgress = state.sectionProgress,
                            onProgressChange = onProgressPreview,
                            onProgressChangeFinished = onProgressChange,
                            previousSectionDescription = stringResource(MR.strings.action_previous_section),
                            nextSectionDescription = stringResource(MR.strings.action_next_section),
                        )
                    }
                    ReadiumReaderBottomBar(
                        paginated = paginated,
                        showLayoutToggle = !state.fixedLayout,
                        onOpenToc = { onTocVisibilityChange(true) },
                        onToggleLayout = {
                            val target = if (paginated) {
                                ReadiumEpubSettingsProvider.LAYOUT_SCROLLING
                            } else {
                                ReadiumEpubSettingsProvider.LAYOUT_PAGINATED
                            }
                            scope.launch { settings.layoutMode.setEntryOverride(target) }
                        },
                        onOpenSettings = { onSettingsVisibilityChange(true) },
                        modifier = Modifier.background(backgroundColor),
                    )
                }
            },
        )

        ReadiumTableOfContentsSheet(
            visible = state.tocVisible,
            navigation = navigation,
            selectedIndex = state.currentSectionIndex,
            onItemClick = onNavigationItemClick,
            onDismissRequest = { onTocVisibilityChange(false) },
        )
    }

    if (state.settingsVisible) {
        ReadiumEpubSettingsDialog(
            settings = settings,
            onDismissRequest = { onSettingsVisibilityChange(false) },
        )
    }

    BackHandler(enabled = state.tocVisible || state.settingsVisible) {
        when {
            state.tocVisible -> onTocVisibilityChange(false)
            else -> onSettingsVisibilityChange(false)
        }
    }
}

@Composable
private fun ReadiumReaderTopBar(
    bookTitle: String,
    sectionTitle: String?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Column {
                Text(bookTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                sectionTitle?.let {
                    Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(MR.strings.action_close),
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
        ),
    )
}

@Composable
private fun ReadiumReaderBottomBar(
    paginated: Boolean,
    showLayoutToggle: Boolean,
    onOpenToc: () -> Unit,
    onToggleLayout: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showLayoutToggle) {
            IconButton(onClick = onToggleLayout) {
                Icon(
                    imageVector = if (paginated) Icons.Outlined.ViewCarousel else Icons.Outlined.ViewStream,
                    contentDescription = stringResource(MR.strings.pref_epub_layout_mode),
                )
            }
        }
        IconButton(onClick = onOpenSettings) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = stringResource(MR.strings.action_settings),
            )
        }
        IconButton(onClick = onOpenToc) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ViewList,
                contentDescription = stringResource(MR.strings.book_table_of_contents),
            )
        }
    }
}

@Composable
private fun ReadiumTableOfContentsSheet(
    visible: Boolean,
    navigation: List<ReadiumNavigationRow>,
    selectedIndex: Int,
    onItemClick: (BookNavigationItem) -> Unit,
    onDismissRequest: () -> Unit,
) {
    if (!visible) return

    val listState = rememberLazyListState()
    LaunchedEffect(visible, selectedIndex) {
        if (visible && selectedIndex >= 0) listState.scrollToItem(selectedIndex)
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = true),
    ) {
        BoxWithConstraints {
            AdaptiveSheet(
                isTabletUi = false,
                enableImplicitDismiss = true,
                onDismissRequest = onDismissRequest,
                modifier = Modifier.heightIn(max = maxHeight * 0.85f),
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ViewList,
                            contentDescription = null,
                        )
                        Text(
                            text = stringResource(MR.strings.book_table_of_contents),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 16.dp),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        IconButton(onClick = onDismissRequest) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = stringResource(MR.strings.action_close),
                            )
                        }
                    }
                    HorizontalDivider()
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        state = listState,
                    ) {
                        itemsIndexed(navigation) { index, row ->
                            val selected = index == selectedIndex
                            Text(
                                text = row.item.title?.takeIf(String::isNotBlank)
                                    ?: row.item.target.resourceId,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (selected) {
                                            MaterialTheme.colorScheme.secondaryContainer
                                        } else {
                                            Color.Transparent
                                        },
                                    )
                                    .clickable {
                                        onItemClick(row.item)
                                        onDismissRequest()
                                    }
                                    .padding(
                                        start = 16.dp + (row.depth * 16).dp,
                                        top = 12.dp,
                                        end = 16.dp,
                                        bottom = 12.dp,
                                    ),
                                color = if (selected) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadiumEpubSettingsDialog(
    settings: ReadiumEpubSettingsBinding,
    onDismissRequest: () -> Unit,
) {
    val tabs = listOf(
        stringResource(MR.strings.pref_category_display),
        stringResource(MR.strings.pref_epub_page_layout),
        stringResource(MR.strings.pref_epub_controls),
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
                                0 -> ReadiumAppearanceSettings(settings)
                                1 -> ReadiumLayoutSettings(settings)
                                2 -> ReadiumControlSettings(settings)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadiumAppearanceSettings(settings: ReadiumEpubSettingsBinding) {
    val theme by settings.theme.state.collectEffectiveValue()
    val fontFamily by settings.fontFamily.state.collectEffectiveValue()
    val fontSize by settings.fontSize.state.collectEffectiveValue()

    SettingChips(
        label = stringResource(MR.strings.pref_epub_color_theme),
        values = listOf(
            ReadiumEpubSettingsProvider.THEME_LIGHT to stringResource(MR.strings.pref_epub_theme_light),
            ReadiumEpubSettingsProvider.THEME_DARK to stringResource(MR.strings.pref_epub_theme_dark),
            ReadiumEpubSettingsProvider.THEME_SEPIA to stringResource(MR.strings.pref_epub_theme_sepia),
        ),
        selected = theme,
        onSelect = settings.theme::setProfileValue,
    )
    SettingChips(
        label = stringResource(MR.strings.pref_epub_font_family),
        values = listOf(
            ReadiumEpubSettingsProvider.FONT_PUBLISHER to stringResource(MR.strings.pref_epub_font_publisher),
            ReadiumEpubSettingsProvider.FONT_SERIF to stringResource(MR.strings.pref_epub_font_serif),
            ReadiumEpubSettingsProvider.FONT_SANS_SERIF to stringResource(MR.strings.pref_epub_font_sans_serif),
            ReadiumEpubSettingsProvider.FONT_MONOSPACE to stringResource(MR.strings.pref_epub_font_monospace),
            ReadiumEpubSettingsProvider.FONT_OPEN_DYSLEXIC to "OpenDyslexic",
        ),
        selected = fontFamily,
        onSelect = settings.fontFamily::setProfileValue,
    )
    SliderItem(
        value = fontSize,
        valueRange = ReadiumEpubSettingsProvider.FONT_SIZE_RANGE step 10,
        steps = 24,
        label = stringResource(MR.strings.pref_epub_font_size),
        valueString = "$fontSize%",
        onChange = settings.fontSize::setProfileValue,
    )
}

@Composable
private fun ReadiumLayoutSettings(settings: ReadiumEpubSettingsBinding) {
    val scope = rememberCoroutineScope()
    val layoutMode by settings.layoutMode.state.collectEffectiveValue()
    val columnCount by settings.columnCount.state.collectEffectiveValue()
    val pageMargins by settings.pageMargins.state.collectEffectiveValue()
    val publisherStyles by settings.publisherStyles.state.collectEffectiveValue()
    val lineHeight by settings.lineHeight.state.collectEffectiveValue()
    val textAlignment by settings.textAlignment.state.collectEffectiveValue()
    val textNormalization by settings.textNormalization.state.collectEffectiveValue()

    SettingChips(
        label = stringResource(MR.strings.pref_epub_layout_mode),
        values = listOf(
            ReadiumEpubSettingsProvider.LAYOUT_PAGINATED to stringResource(MR.strings.pref_epub_layout_paginated),
            ReadiumEpubSettingsProvider.LAYOUT_SCROLLING to stringResource(MR.strings.pref_epub_layout_scrolling),
        ),
        selected = layoutMode,
        onSelect = { scope.launch { settings.layoutMode.setEntryOverride(it) } },
    )
    if (layoutMode == ReadiumEpubSettingsProvider.LAYOUT_PAGINATED) {
        SettingChips(
            label = stringResource(MR.strings.pref_epub_column_count),
            values = listOf(
                ReadiumEpubSettingsProvider.COLUMNS_AUTO to stringResource(MR.strings.pref_epub_columns_auto),
                ReadiumEpubSettingsProvider.COLUMNS_ONE to stringResource(MR.strings.pref_epub_columns_one),
                ReadiumEpubSettingsProvider.COLUMNS_TWO to stringResource(MR.strings.pref_epub_columns_two),
            ),
            selected = columnCount,
            onSelect = settings.columnCount::setProfileValue,
        )
    }
    SliderItem(
        value = pageMargins,
        valueRange = ReadiumEpubSettingsProvider.PAGE_MARGINS_RANGE step 20,
        steps = 19,
        label = stringResource(MR.strings.pref_epub_page_margins),
        valueString = "$pageMargins%",
        onChange = settings.pageMargins::setProfileValue,
    )
    CheckboxItem(
        label = stringResource(MR.strings.pref_epub_publisher_styles),
        checked = publisherStyles,
        onClick = { settings.publisherStyles.setProfileValue(!publisherStyles) },
    )
    if (!publisherStyles) {
        SliderItem(
            value = lineHeight,
            valueRange = ReadiumEpubSettingsProvider.LINE_HEIGHT_RANGE step 10,
            steps = 9,
            label = stringResource(MR.strings.pref_epub_line_height),
            valueString = "$lineHeight%",
            onChange = settings.lineHeight::setProfileValue,
        )
        SettingChips(
            label = stringResource(MR.strings.pref_epub_text_alignment),
            values = listOf(
                ReadiumEpubSettingsProvider.ALIGN_PUBLISHER to
                    stringResource(MR.strings.pref_epub_alignment_publisher),
                ReadiumEpubSettingsProvider.ALIGN_START to stringResource(MR.strings.pref_epub_alignment_start),
                ReadiumEpubSettingsProvider.ALIGN_JUSTIFY to stringResource(MR.strings.pref_epub_alignment_justify),
                ReadiumEpubSettingsProvider.ALIGN_LEFT to stringResource(MR.strings.pref_epub_alignment_left),
                ReadiumEpubSettingsProvider.ALIGN_RIGHT to stringResource(MR.strings.pref_epub_alignment_right),
            ),
            selected = textAlignment,
            onSelect = settings.textAlignment::setProfileValue,
        )
    }
    CheckboxItem(
        label = stringResource(MR.strings.pref_epub_text_normalization),
        checked = textNormalization,
        onClick = { settings.textNormalization.setProfileValue(!textNormalization) },
    )
}

@Composable
private fun ReadiumControlSettings(settings: ReadiumEpubSettingsBinding) {
    val tapNavigation by settings.tapNavigation.state.collectEffectiveValue()
    val showPageNumber by settings.showPageNumber.state.collectEffectiveValue()
    CheckboxItem(
        label = stringResource(MR.strings.pref_epub_tap_navigation),
        checked = tapNavigation,
        onClick = { settings.tapNavigation.setProfileValue(!tapNavigation) },
    )
    CheckboxItem(
        label = stringResource(MR.strings.pref_epub_show_reading_progress),
        checked = showPageNumber,
        onClick = { settings.showPageNumber.setProfileValue(!showPageNumber) },
    )
}

@Composable
private fun SettingChips(
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
                FilterChip(
                    selected = selected == value,
                    onClick = { onSelect(value) },
                    label = { Text(text) },
                )
            }
        }
    }
}

@Composable
private fun <T> StateFlow<ResolvedViewerSetting<T>>.collectEffectiveValue(): State<T> {
    val resolved by collectAsState()
    return rememberUpdatedState(resolved.effectiveValue)
}

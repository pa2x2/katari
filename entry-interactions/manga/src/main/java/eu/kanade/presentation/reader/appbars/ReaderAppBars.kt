package eu.kanade.presentation.reader.appbars

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import eu.kanade.presentation.reader.components.ChapterNavigator
import eu.kanade.presentation.reader.components.ChapterNavigatorType
import mihon.entry.interactions.reader.settings.ReaderOrientation
import mihon.entry.interactions.reader.settings.ReadingMode
import mihon.entry.interactions.source.EntryChildWebViewAction
import mihon.entry.interactions.source.EntryChildWebViewResolution
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.components.reader.ReaderChrome

private val readerBarsSlideAnimationSpec = tween<IntOffset>(200)
private val readerBarsFadeAnimationSpec = tween<Float>(150)

@Composable
internal fun ReaderAppBars(
    visible: Boolean,

    mangaTitle: String?,
    chapterTitle: String?,
    navigateUp: () -> Unit,
    onClickTopAppBar: () -> Unit,
    bookmarked: Boolean,
    onToggleBookmarked: () -> Unit,
    childWebView: EntryChildWebViewResolution.Available?,
    onChildWebViewAction: (EntryChildWebViewAction, EntryChildWebViewResolution.Available) -> Unit,

    chapterNavigatorType: ChapterNavigatorType,
    verticalNavigatorHeight: Float,
    onNextChapter: () -> Unit,
    enabledNext: Boolean,
    onPreviousChapter: () -> Unit,
    enabledPrevious: Boolean,
    currentPage: Int,
    totalPages: Int,
    onPageIndexChange: (Int) -> Unit,
    onPageIndexChangeFinished: () -> Unit,

    readingMode: ReadingMode,
    onClickReadingMode: () -> Unit,
    orientation: ReaderOrientation,
    onClickOrientation: () -> Unit,
    cropEnabled: Boolean,
    onClickCropBorder: () -> Unit,
    showAutoScrollToggle: Boolean,
    autoScrollActive: Boolean,
    onClickAutoScroll: () -> Unit,
    onClickSettings: () -> Unit,
) {
    ReaderChrome(
        visible = visible,
        topBar = {
            ReaderTopBar(
                modifier = Modifier
                    .clickable(onClick = onClickTopAppBar),
                mangaTitle = mangaTitle,
                chapterTitle = chapterTitle,
                navigateUp = navigateUp,
                bookmarked = bookmarked,
                onToggleBookmarked = onToggleBookmarked,
                childWebView = childWebView,
                onChildWebViewAction = onChildWebViewAction,
            )
        },
        middleContent = {
            if (!chapterNavigatorType.isHorizontal()) {
                val sliderOnLeft = chapterNavigatorType == ChapterNavigatorType.VERTICAL_LEFT
                CompositionLocalProvider(
                    LocalLayoutDirection provides if (sliderOnLeft) LayoutDirection.Ltr else LayoutDirection.Rtl,
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        AnimatedVisibility(
                            visible = visible,
                            enter = slideInHorizontally(readerBarsSlideAnimationSpec) {
                                if (sliderOnLeft) -it else it
                            } + fadeIn(readerBarsFadeAnimationSpec),
                            exit = slideOutHorizontally(readerBarsSlideAnimationSpec) {
                                if (sliderOnLeft) -it else it
                            } + fadeOut(readerBarsFadeAnimationSpec),
                        ) {
                            Row {
                                Spacer(modifier = Modifier.width(MaterialTheme.padding.small))
                                Box(
                                    modifier = Modifier.fillMaxHeight(),
                                    contentAlignment = Alignment.BottomCenter,
                                ) {
                                    ChapterNavigator(
                                        modifier = Modifier.fillMaxHeight(verticalNavigatorHeight),
                                        type = chapterNavigatorType,
                                        onNextChapter = onNextChapter,
                                        enabledNext = enabledNext,
                                        onPreviousChapter = onPreviousChapter,
                                        enabledPrevious = enabledPrevious,
                                        currentPage = currentPage,
                                        totalPages = totalPages,
                                        onPageIndexChange = onPageIndexChange,
                                        onPageIndexChangeFinished = onPageIndexChangeFinished,
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        },
        bottomBar = {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small)) {
                if (chapterNavigatorType.isHorizontal()) {
                    ChapterNavigator(
                        type = chapterNavigatorType,
                        onNextChapter = onNextChapter,
                        enabledNext = enabledNext,
                        onPreviousChapter = onPreviousChapter,
                        enabledPrevious = enabledPrevious,
                        currentPage = currentPage,
                        totalPages = totalPages,
                        onPageIndexChange = onPageIndexChange,
                        onPageIndexChangeFinished = onPageIndexChangeFinished,
                    )
                }
                ReaderBottomBar(
                    readingMode = readingMode,
                    onClickReadingMode = onClickReadingMode,
                    orientation = orientation,
                    onClickOrientation = onClickOrientation,
                    cropEnabled = cropEnabled,
                    onClickCropBorder = onClickCropBorder,
                    showAutoScrollToggle = showAutoScrollToggle,
                    autoScrollActive = autoScrollActive,
                    onClickAutoScroll = onClickAutoScroll,
                    onClickSettings = onClickSettings,
                )
            }
        },
    )
}

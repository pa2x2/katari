package mihon.entry.interactions.book.epub

import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import mihon.book.api.BookLocator
import mihon.book.api.BookNavigationItem
import mihon.book.api.BookReadingDirection
import mihon.entry.interactions.EntryChildWebViewAction
import mihon.entry.interactions.EntryChildWebViewResolution
import mihon.entry.interactions.book.BookReaderNavigationRow
import mihon.entry.interactions.book.BookReaderNavigationSheet
import mihon.entry.interactions.book.BookReaderProgress
import mihon.entry.interactions.book.BookReaderScaffold
import mihon.entry.interactions.book.BookSelectionTranslationController
import mihon.entry.interactions.settings.ReadiumEpubSettingsProvider
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.reader.ReaderChrome
import tachiyomi.presentation.core.components.reader.ReaderPageNavigator
import tachiyomi.presentation.core.components.reader.ReaderPageNavigatorType
import tachiyomi.presentation.core.i18n.stringResource

internal data class ReadiumEpubReaderUiState(
    val bookTitle: String,
    val sectionTitle: String? = null,
    val currentLocator: BookLocator? = null,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val currentSectionIndex: Int = -1,
    val sectionCount: Int = 0,
    val readingDirection: BookReadingDirection? = null,
    val menuVisible: Boolean = false,
    val tocVisible: Boolean = false,
    val settingsVisible: Boolean = false,
    val childWebView: EntryChildWebViewResolution.Available? = null,
)

@Composable
internal fun ReadiumEpubReaderScreen(
    state: ReadiumEpubReaderUiState,
    navigation: List<ReadiumNavigationRow>,
    settings: ReadiumEpubSettingsBinding,
    nativeContentView: View,
    onClose: () -> Unit,
    onTocVisibilityChange: (Boolean) -> Unit,
    onSettingsVisibilityChange: (Boolean) -> Unit,
    onPageIndexPreview: (Int) -> Unit,
    onPageIndexChange: (Int) -> Unit,
    onPreviousSection: () -> Unit,
    onNextSection: () -> Unit,
    onNavigationItemClick: (BookNavigationItem) -> Unit,
    onChildWebViewAction: (EntryChildWebViewAction, EntryChildWebViewResolution.Available) -> Unit,
    translationController: BookSelectionTranslationController? = null,
    onReaderRootPositionInWindow: (Offset) -> Unit = {},
) {
    val theme by settings.theme.state.collectEffectiveValue()
    val showPageNumber by settings.showPageNumber.state.collectEffectiveValue()
    val footerColor = when (theme) {
        ReadiumEpubSettingsProvider.THEME_DARK -> Color(0xFF121212)
        ReadiumEpubSettingsProvider.THEME_SEPIA -> Color(0xFFF4ECD8)
        else -> Color.White
    }

    BookReaderScaffold(
        progress = if (!showPageNumber) {
            null
        } else {
            BookReaderProgress.Page(state.currentPage, state.totalPages)
        },
        progressVisible = !state.menuVisible,
        footerColor = footerColor,
        modifier = Modifier.fillMaxSize(),
        nativeContentView = nativeContentView,
        translationController = translationController,
        onRootPositionInWindow = onReaderRootPositionInWindow,
        content = {},
        overlay = {
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
                        childWebView = state.childWebView,
                        onChildWebViewAction = onChildWebViewAction,
                        modifier = Modifier.background(backgroundColor),
                    )
                },
                bottomBar = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            showSinglePageLabel = true,
                            previousSectionDescription = stringResource(MR.strings.action_previous_section),
                            nextSectionDescription = stringResource(MR.strings.action_next_section),
                        )
                        ReadiumReaderBottomBar(
                            onOpenToc = { onTocVisibilityChange(true) },
                            onOpenSettings = { onSettingsVisibilityChange(true) },
                            modifier = Modifier.background(backgroundColor),
                        )
                    }
                },
            )

            BookReaderNavigationSheet(
                visible = state.tocVisible,
                rows = navigation.map { row ->
                    BookReaderNavigationRow(
                        item = row.item,
                        title = row.item.title?.takeIf(String::isNotBlank) ?: row.item.target.resourceId,
                        depth = row.depth,
                    )
                },
                selectedIndex = state.currentSectionIndex,
                onItemClick = onNavigationItemClick,
                onDismissRequest = { onTocVisibilityChange(false) },
            )
        },
    )

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

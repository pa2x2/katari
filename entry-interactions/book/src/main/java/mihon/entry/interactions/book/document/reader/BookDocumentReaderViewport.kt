package mihon.entry.interactions.book.document.reader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import mihon.book.api.document.BookDocumentLinkTarget
import mihon.entry.interactions.viewer.EntryChildWindow
import tachiyomi.domain.entry.model.EntryChapter

/** Binds reader settings to the mode-independent viewport. */
@Composable
internal fun BookDocumentReaderViewport(
    currentChapter: EntryChapter,
    currentChapterId: Long,
    window: EntryChildWindow<EntryChapter>,
    loadedSections: Map<Long, BookDocumentPublicationSections<EntryChapter>>,
    loadStates: Map<Long, BookDocumentChapterLoadState>,
    navigationRequest: BookDocumentNavigationRequest?,
    textSizePercent: Int,
    onLocation: (BookDocumentViewerLocation<EntryChapter>) -> Unit,
    onTransitionReached: (EntryChapter) -> Unit,
    onTerminalObservation: (EntryChapter, Boolean, Boolean, Boolean) -> Unit,
    onAnchorMissing: (String) -> Unit,
    onInternalLinkClick: (BookDocumentSection<EntryChapter>, BookDocumentLinkTarget) -> Unit,
    onExternalLinkClick: (String) -> Unit,
    onScrollStarted: () -> Unit,
    onUserScrollStarted: () -> Unit,
    onReaderTap: () -> Unit,
    settings: mihon.entry.interactions.book.document.reader.settings.BookDocumentReaderSettingBindings,
    chromeVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    val mode by settings.readingMode.state.collectAsState()
    val tapZones by settings.tapZones.state.collectAsState()
    val inversion by settings.tapInversion.state.collectAsState()
    val animation by settings.animatePages.state.collectAsState()
    val volume by settings.volumeKeys.state.collectAsState()
    val invertVolume by settings.invertVolumeKeys.state.collectAsState()
    BookDocumentModeViewport(
        currentChapter, currentChapterId, window, loadedSections, loadStates, navigationRequest, textSizePercent,
        onLocation, onTransitionReached, onTerminalObservation, onAnchorMissing, onInternalLinkClick,
        onExternalLinkClick, onScrollStarted, onUserScrollStarted, onReaderTap,
        mode.effectiveValue, tapZones.effectiveValue, inversion.effectiveValue, animation.effectiveValue,
        volume.effectiveValue, invertVolume.effectiveValue, chromeVisible, modifier,
    )
}

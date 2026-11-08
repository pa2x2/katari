package app.katari.entry.interactions.runtime

import app.katari.entry.interactions.child.EntryChildListInteraction
import app.katari.entry.interactions.child.EntryChildProgressInteraction
import app.katari.entry.interactions.child.EntryMissingChildGapInteraction
import app.katari.entry.interactions.child.EntryScanlatorFilterInteraction
import app.katari.entry.interactions.download.EntryDownloadInteraction
import app.katari.entry.interactions.library.EntryLibraryProgressInteraction
import app.katari.entry.interactions.media.EntryImmersiveInteraction
import app.katari.entry.interactions.media.EntryMediaCacheInteraction
import app.katari.entry.interactions.media.EntryPreviewInteraction
import app.katari.entry.interactions.media.EntryViewerSettingsInteraction
import app.katari.entry.interactions.navigation.EntryContinueInteraction
import app.katari.entry.interactions.navigation.EntryOpenInteraction
import app.katari.entry.interactions.presentation.EntryTypePresentationInteraction
import app.katari.entry.interactions.state.EntryBookmarkInteraction
import app.katari.entry.interactions.state.EntryConsumptionInteraction
import app.katari.entry.interactions.state.EntryPlaybackPreferencesInteraction
import app.katari.entry.interactions.state.EntryProgressInteraction
import app.katari.entry.interactions.statistics.EntryStatisticsInteraction
import app.katari.entry.interactions.translate.EntryTranslateInteraction

/** Internal operational dispatch assembled from contributed type providers. */
interface EntryInteractions {
    val open: EntryOpenInteraction
    val continueEntry: EntryContinueInteraction
    val download: EntryDownloadInteraction
    val consumption: EntryConsumptionInteraction
    val bookmark: EntryBookmarkInteraction
    val preview: EntryPreviewInteraction
    val immersive: EntryImmersiveInteraction
    val progress: EntryProgressInteraction
    val playbackPreferences: EntryPlaybackPreferencesInteraction
    val childList: EntryChildListInteraction
    val childProgress: EntryChildProgressInteraction
    val missingChildGap: EntryMissingChildGapInteraction
    val scanlatorFilter: EntryScanlatorFilterInteraction
    val libraryProgress: EntryLibraryProgressInteraction
    val typePresentation: EntryTypePresentationInteraction
    val viewerSettings: EntryViewerSettingsInteraction
    val mediaCache: EntryMediaCacheInteraction
    val statistics: EntryStatisticsInteraction
    val translate: EntryTranslateInteraction
}

package mihon.entry.interactions.runtime

import mihon.entry.interactions.child.EntryChildGroupFilterInteraction
import mihon.entry.interactions.child.EntryChildListInteraction
import mihon.entry.interactions.child.EntryChildProgressInteraction
import mihon.entry.interactions.child.EntryMissingChildGapInteraction
import mihon.entry.interactions.download.EntryDownloadInteraction
import mihon.entry.interactions.library.EntryLibraryProgressInteraction
import mihon.entry.interactions.media.EntryImmersiveInteraction
import mihon.entry.interactions.media.EntryMediaCacheInteraction
import mihon.entry.interactions.media.EntryPreviewInteraction
import mihon.entry.interactions.media.EntryViewerSettingsInteraction
import mihon.entry.interactions.navigation.EntryContinueInteraction
import mihon.entry.interactions.navigation.EntryOpenInteraction
import mihon.entry.interactions.presentation.EntryTypePresentationInteraction
import mihon.entry.interactions.state.EntryBookmarkInteraction
import mihon.entry.interactions.state.EntryConsumptionInteraction
import mihon.entry.interactions.state.EntryPlaybackPreferencesInteraction
import mihon.entry.interactions.state.EntryProgressInteraction
import mihon.entry.interactions.statistics.EntryStatisticsInteraction

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
    val childGroupFilter: EntryChildGroupFilterInteraction
    val libraryProgress: EntryLibraryProgressInteraction
    val typePresentation: EntryTypePresentationInteraction
    val viewerSettings: EntryViewerSettingsInteraction
    val mediaCache: EntryMediaCacheInteraction
    val statistics: EntryStatisticsInteraction
}

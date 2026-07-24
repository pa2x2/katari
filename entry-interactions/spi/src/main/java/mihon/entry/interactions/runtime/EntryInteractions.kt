package mihon.entry.interactions

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
}

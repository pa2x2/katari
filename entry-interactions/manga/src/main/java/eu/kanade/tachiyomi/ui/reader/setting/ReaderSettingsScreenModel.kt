package eu.kanade.tachiyomi.ui.reader.setting

import eu.kanade.tachiyomi.ui.reader.ReaderViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import mihon.entry.interactions.manga.reader.settings.MangaReaderSettingsBindings
import mihon.entry.interactions.reader.settings.ReaderOrientation
import mihon.entry.interactions.reader.settings.ReadingMode
import mihon.entry.viewer.settings.ViewerSettingSource

internal class ReaderSettingsScreenModel(
    private val readerState: StateFlow<ReaderViewModel.State>,
    val settings: MangaReaderSettingsBindings,
    val onChangeReadingMode: (ReadingMode) -> Unit,
    val onChangeOrientation: (ReaderOrientation) -> Unit,
) {

    private val ioCoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val viewerFlow = readerState
        .map { it.viewer }
        .distinctUntilChanged()
        .stateIn(ioCoroutineScope, SharingStarted.Lazily, null)

    /**
     * Exposes the override (or [ReadingMode.DEFAULT] when none is set) so the "for this series" section keeps its
     * explicit default choice.
     */
    val readingModeFlow = settings.readingMode.state
        .map { resolved ->
            if (resolved.source == ViewerSettingSource.ENTRY) {
                ReadingMode.fromPreference(resolved.effectiveValue)
            } else {
                ReadingMode.DEFAULT
            }
        }
        .distinctUntilChanged()
        .stateIn(ioCoroutineScope, SharingStarted.Lazily, ReadingMode.DEFAULT)

    val orientationFlow = settings.orientation.state
        .map { resolved ->
            if (resolved.source == ViewerSettingSource.ENTRY) {
                ReaderOrientation.fromPreference(resolved.effectiveValue)
            } else {
                ReaderOrientation.DEFAULT
            }
        }
        .distinctUntilChanged()
        .stateIn(ioCoroutineScope, SharingStarted.Lazily, ReaderOrientation.DEFAULT)

    /** Clears this series' overrides while keeping profile values, matching the book reader reset semantics. */
    suspend fun clearEntryOverrides() {
        settings.clearEntryOverrides()
    }
}

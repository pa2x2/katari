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
import kotlinx.coroutines.launch
import mihon.entry.interactions.reader.preparation.ReaderChapterPreparationPreferences
import mihon.entry.interactions.reader.settings.MangaReaderSettingsProvider
import mihon.entry.interactions.reader.settings.ReaderOrientation
import mihon.entry.interactions.reader.settings.ReadingMode
import mihon.entry.viewer.settings.ViewerSettingBinder
import mihon.entry.viewer.settings.resetSettings
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal class ReaderSettingsScreenModel(
    private val readerState: StateFlow<ReaderViewModel.State>,
    val onChangeReadingMode: (ReadingMode) -> Unit,
    val onChangeOrientation: (ReaderOrientation) -> Unit,
    val preferences: MangaReaderSettingsProvider = Injekt.get(),
    chapterPreparationPreferences: ReaderChapterPreparationPreferences = Injekt.get(),
    private val settingBinder: ViewerSettingBinder = Injekt.get(),
) {

    private val ioCoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val prepareNextChapter = chapterPreparationPreferences.prepareNextChapter(
        MangaReaderSettingsProvider.PROVIDER_ID,
    )

    val viewerFlow = readerState
        .map { it.viewer }
        .distinctUntilChanged()
        .stateIn(ioCoroutineScope, SharingStarted.Lazily, null)

    val readingModeFlow = readerState
        .map { ReadingMode.fromPreference(it.readingModeOverride) }
        .distinctUntilChanged()
        .stateIn(ioCoroutineScope, SharingStarted.Lazily, ReadingMode.DEFAULT)

    val orientationFlow = readerState
        .map { ReaderOrientation.fromPreference(it.orientationOverride) }
        .distinctUntilChanged()
        .stateIn(ioCoroutineScope, SharingStarted.Lazily, ReaderOrientation.DEFAULT)

    fun resetSettings() {
        ioCoroutineScope.launch {
            settingBinder.resetSettings(
                provider = preferences,
                entryId = readerState.value.manga?.id,
            )
            prepareNextChapter.delete()
        }
    }
}

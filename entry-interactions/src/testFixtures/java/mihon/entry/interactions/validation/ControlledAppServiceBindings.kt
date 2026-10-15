package mihon.entry.interactions.validation

import android.app.Application
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.network.NetworkHelper
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json
import mihon.core.common.image.progressive.ProgressiveImageEngine
import mihon.core.metro.AppCoroutineScope
import mihon.entry.interactions.translate.EntryTranslatePreferences
import mihon.entry.viewer.settings.ViewerSettingOverrideRepository
import nl.adaptivity.xmlutil.serialization.XML
import okhttp3.OkHttpClient
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.domain.category.repository.CategoryRepository
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.entry.interactor.SyncEntryWithSource
import tachiyomi.domain.entry.repository.DownloadPreferencesRepository
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.repository.EntryTranslationLanguagesRepository
import tachiyomi.domain.entry.repository.EntryTranslationQueueRepository
import tachiyomi.domain.entry.repository.PlaybackPreferencesRepository
import tachiyomi.domain.history.repository.HistoryRepository
import tachiyomi.domain.library.service.GlobalLibraryPreferences
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.storage.service.StorageManager

/**
 * The app services production validation graphs run the real Features on: the application and scope the validation
 * environment owns, and controlled substitutes for storage, network, sources and the data layer. Production provides
 * them from the app graph's own bindings; the graph builds the domain interactors on top of these.
 */
@BindingContainer
class ControlledAppServiceBindings(
    private val application: Application,
    private val appScope: CoroutineScope,
) {
    @Provides
    fun application(): Application = application

    @Provides
    @AppCoroutineScope
    fun appCoroutineScope(): CoroutineScope = appScope

    @Provides
    fun networkHelper(): NetworkHelper = mockk(relaxed = true) {
        every { client } returns mockk<OkHttpClient>(relaxed = true)
    }

    @Provides
    fun storageManager(): StorageManager = mockk(relaxed = true) {
        every { changes } returns MutableSharedFlow()
        every { getDownloadsDirectory() } returns null
    }

    @Provides
    fun sourceManager(): SourceManager = mockk(relaxed = true) {
        every { isInitialized } returns MutableStateFlow(true)
    }

    @Provides
    fun json(): Json = Json

    @Provides
    fun xml(): XML = XML.v1 {}

    @Provides
    fun preferenceStore(): PreferenceStore = InMemoryPreferenceStore()

    @Provides
    fun translatePreferences(): EntryTranslatePreferences = EntryTranslatePreferences(InMemoryPreferenceStore())

    @Provides
    fun securityPreferences(): SecurityPreferences = mockk(relaxed = true)

    @Provides
    fun globalLibraryPreferences(): GlobalLibraryPreferences = mockk(relaxed = true)

    @Provides
    fun downloadPreferences(): DownloadPreferences = mockk(relaxed = true)

    @Provides
    fun progressiveImageEngine(): ProgressiveImageEngine = mockk(relaxed = true)

    @Provides
    fun syncEntryWithSource(): SyncEntryWithSource = mockk(relaxed = true)

    @Provides
    fun categoryRepository(): CategoryRepository = mockk(relaxed = true)

    @Provides
    fun entryRepository(): EntryRepository = mockk(relaxed = true)

    @Provides
    fun entryChapterRepository(): EntryChapterRepository = mockk(relaxed = true)

    @Provides
    fun entryProgressRepository(): EntryProgressRepository = mockk(relaxed = true)

    @Provides
    fun historyRepository(): HistoryRepository = mockk(relaxed = true)

    @Provides
    fun playbackPreferencesRepository(): PlaybackPreferencesRepository = mockk(relaxed = true)

    @Provides
    fun downloadPreferencesRepository(): DownloadPreferencesRepository = mockk(relaxed = true)

    @Provides
    fun viewerSettingOverrideRepository(): ViewerSettingOverrideRepository = mockk(relaxed = true)

    @Provides
    fun translationQueueRepository(): EntryTranslationQueueRepository = mockk(relaxed = true)

    @Provides
    fun translationLanguagesRepository(): EntryTranslationLanguagesRepository = mockk(relaxed = true)
}

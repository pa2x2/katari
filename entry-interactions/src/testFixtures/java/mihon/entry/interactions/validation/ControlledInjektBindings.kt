package mihon.entry.interactions.validation

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import mihon.core.common.image.progressive.ProgressiveImageEngine
import mihon.core.metro.AppCoroutineScope
import mihon.entry.interactions.translate.EntryTranslatePreferences
import mihon.entry.viewer.settings.ViewerSettingOverrideRepository
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
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * App services the validation graphs read from the controlled Injekt registrations the validation environment
 * installs. Production provides them from the app graph's own bindings; the graph builds the domain interactors on
 * top of these.
 */
@BindingContainer
object ControlledInjektBindings {
    @Provides
    fun networkHelper(): NetworkHelper = Injekt.get()

    @Provides
    fun categoryRepository(): CategoryRepository = Injekt.get()

    @Provides
    fun preferenceStore(): PreferenceStore = Injekt.get()

    @Provides
    fun storageManager(): StorageManager = Injekt.get()

    @Provides
    fun json(): Json = Injekt.get()

    @Provides
    fun progressiveImageEngine(): ProgressiveImageEngine = Injekt.get()

    @Provides
    fun playbackPreferencesRepository(): PlaybackPreferencesRepository = Injekt.get()

    @Provides
    @AppCoroutineScope
    fun appCoroutineScope(): CoroutineScope = Injekt.get()

    @Provides
    fun sourceManager(): SourceManager = Injekt.get()

    @Provides
    fun entryRepository(): EntryRepository = Injekt.get()

    @Provides
    fun entryChapterRepository(): EntryChapterRepository = Injekt.get()

    @Provides
    fun entryProgressRepository(): EntryProgressRepository = Injekt.get()

    @Provides
    fun historyRepository(): HistoryRepository = Injekt.get()

    @Provides
    fun viewerSettingOverrideRepository(): ViewerSettingOverrideRepository = Injekt.get()

    @Provides
    fun globalLibraryPreferences(): GlobalLibraryPreferences = Injekt.get()

    @Provides
    fun syncEntryWithSource(): SyncEntryWithSource = Injekt.get()

    @Provides
    fun downloadPreferencesRepository(): DownloadPreferencesRepository = Injekt.get()

    @Provides
    fun translationQueueRepository(): EntryTranslationQueueRepository = Injekt.get()

    @Provides
    fun translationLanguagesRepository(): EntryTranslationLanguagesRepository = Injekt.get()

    @Provides
    fun downloadPreferences(): DownloadPreferences = Injekt.get()

    @Provides
    fun translatePreferences(): EntryTranslatePreferences = Injekt.get()

    @Provides
    fun securityPreferences(): SecurityPreferences = Injekt.get()
}

package mihon.entry.interactions.validation

import android.app.Application
import eu.kanade.tachiyomi.network.NetworkHelper
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import mihon.entry.interactions.runtime.EntryInteractionComposition
import mihon.entry.interactions.translate.EntryTranslatePreferences
import mihon.entry.viewer.settings.ViewerSettingOverrideRepository
import mihon.feature.runtime.FeatureRuntimeComposition
import mihon.feature.runtime.FeatureRuntimeGraph
import mihon.feature.runtime.addFeatureRuntimeGraph
import nl.adaptivity.xmlutil.serialization.XML
import okhttp3.OkHttpClient
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.core.common.preference.ProfilePreferenceOwnerInstaller
import tachiyomi.core.common.preference.ProfilePreferenceOwnerRegistry
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.repository.CategoryRepository
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.entry.interactor.GetEntry
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.interactor.NetworkToLocalEntry
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
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.storage.service.StorageManager
import tachiyomi.domain.track.interactor.GetTracks
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektScope
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.registry.default.DefaultRegistrar
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class ProductionEntryInteractionValidationEnvironment(
    private val temporaryDirectory: File,
) : AutoCloseable {
    private val previousInjekt: InjektScope = Injekt
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val application: Application = mockk<Application>(relaxed = true).also { application ->
        every { application.cacheDir } returns temporaryDirectory.resolve("cache").also(File::mkdirs)
        every { application.filesDir } returns temporaryDirectory.resolve("files").also(File::mkdirs)
        // Production graph validation runs without Android system services. Keep that boundary
        // explicit so generic service lookups return absence instead of relaxed mock placeholders.
        every { application.getSystemService(any<Class<Any>>()) } returns null
    }

    private val networkHelper = mockk<NetworkHelper>(relaxed = true) {
        every { client } returns mockk<OkHttpClient>(relaxed = true)
    }

    val profilePreferenceOwners = ProfilePreferenceOwnerInstaller(
        owners = ProfilePreferenceOwnerRegistry(),
        preferenceStore = ::InMemoryPreferenceStore,
    )

    init {
        Injekt = InjektScope(DefaultRegistrar())
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    fun composition(graph: FeatureRuntimeGraph): EntryInteractionComposition {
        installControlledRuntimeDependencies()
        Injekt.addFeatureRuntimeGraph(graph)
        graph.featureRuntimeContributions.validate(Injekt.get<FeatureRuntimeComposition>().evaluation)
        return Injekt.get<EntryInteractionComposition>()
    }

    override fun close() {
        appScope.cancel()
        Dispatchers.resetMain()
        Injekt = previousInjekt
    }

    private fun installControlledRuntimeDependencies() {
        val storageManager = mockk<StorageManager>(relaxed = true) {
            every { changes } returns MutableSharedFlow()
            every { getDownloadsDirectory() } returns null
        }
        Injekt.addSingletonFactory<Application> { application }
        Injekt.addSingletonFactory<CoroutineScope> { appScope }
        Injekt.addSingletonFactory<StorageManager> { storageManager }
        Injekt.addSingletonFactory<NetworkHelper> { networkHelper }
        Injekt.addSingletonFactory<Json> { Json }
        Injekt.addSingletonFactory<XML> { XML.v1 {} }
        Injekt.addSingletonFactory<GlobalLibraryPreferences> { mockk(relaxed = true) }
        Injekt.addSingletonFactory<LibraryPreferences> { mockk(relaxed = true) }
        Injekt.addSingletonFactory<SourceManager> {
            mockk(relaxed = true) {
                every { isInitialized } returns MutableStateFlow(true)
            }
        }
        Injekt.addSingletonFactory<ViewerSettingOverrideRepository> { mockk(relaxed = true) }
        Injekt.addSingletonFactory<SyncEntryWithSource> { mockk(relaxed = true) }
        Injekt.addSingletonFactory<EntryChapterRepository> { mockk(relaxed = true) }
        Injekt.addSingletonFactory<EntryProgressRepository> { mockk(relaxed = true) }
        Injekt.addSingletonFactory<EntryRepository> { mockk(relaxed = true) }
        Injekt.addSingletonFactory<DownloadPreferences> { mockk(relaxed = true) }
        Injekt.addSingletonFactory<DownloadPreferencesRepository> { mockk(relaxed = true) }
        Injekt.addSingletonFactory<PlaybackPreferencesRepository> { mockk(relaxed = true) }
        Injekt.addSingletonFactory<EntryTranslationLanguagesRepository> { mockk(relaxed = true) }
        Injekt.addSingletonFactory<EntryTranslationQueueRepository> { mockk(relaxed = true) }
        Injekt.addSingletonFactory { EntryTranslatePreferences(InMemoryPreferenceStore()) }
        Injekt.addSingletonFactory<HistoryRepository> { mockk(relaxed = true) }
        Injekt.addSingletonFactory<CategoryRepository> { mockk(relaxed = true) }
        Injekt.addSingletonFactory<GetEntryWithChapters> { mockk(relaxed = true) }
        Injekt.addSingletonFactory<GetEntry> { mockk(relaxed = true) }
        Injekt.addSingletonFactory<NetworkToLocalEntry> { mockk(relaxed = true) }
        Injekt.addSingletonFactory<GetCategories> { mockk(relaxed = true) }
        Injekt.addSingletonFactory<GetTracks> { mockk(relaxed = true) }
    }
}

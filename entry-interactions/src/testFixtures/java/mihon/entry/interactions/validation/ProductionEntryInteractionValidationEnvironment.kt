package mihon.entry.interactions.validation

import android.app.Application
import eu.kanade.tachiyomi.network.NetworkHelper
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import mihon.entry.interactions.EntryDestructiveRemovalCustomCoverHost
import mihon.entry.interactions.EntryDestructiveRemovalHost
import mihon.entry.interactions.EntryDownloadLifecycleEventSink
import mihon.entry.interactions.EntryDownloadNotificationActions
import mihon.entry.interactions.EntryDownloadWorkController
import mihon.entry.interactions.EntryFeatureRuntimeInstallation
import mihon.entry.interactions.EntryInteractionActivityTheme
import mihon.entry.interactions.EntryInteractionComposition
import mihon.entry.interactions.EntryInteractionRuntimeDependencies
import mihon.entry.interactions.EntryLibraryCustomCoverHost
import mihon.entry.interactions.EntryLibraryMembershipHost
import mihon.entry.interactions.EntryMediaSessionIncognitoState
import mihon.entry.interactions.EntryPageImageCache
import mihon.entry.interactions.EntryProfileMoveChildGroupFilterStateHost
import mihon.entry.interactions.EntryProfileMoveCoverHashStateHost
import mihon.entry.interactions.EntryProfileMoveCustomCoverHost
import mihon.entry.interactions.EntryProfileMoveHost
import mihon.entry.interactions.EntryProfileMoveSourceVisibilityHost
import mihon.entry.interactions.EntryProfileMoveTrackingStateHost
import mihon.entry.interactions.EntryViewerSettingsScreenProjection
import mihon.entry.interactions.EntryViewerSettingsScreenProjectionResolver
import mihon.entry.interactions.addEntryInteractionRuntime
import mihon.entry.interactions.host.EntryMergeHost
import mihon.entry.interactions.host.EntryMigrationConsequenceHost
import mihon.entry.interactions.host.EntryMigrationCustomCoverHost
import mihon.entry.interactions.host.EntryMigrationExecutionHost
import mihon.entry.interactions.host.EntryMigrationPreparationHost
import mihon.entry.interactions.host.tracking.EntryTrackingHost
import mihon.entry.interactions.settings.EntryInteractionPreferences
import mihon.entry.interactions.validateInstalledEntryFeatureRuntimeModules
import mihon.entry.viewer.settings.ViewerSettingOverrideRepository
import nl.adaptivity.xmlutil.serialization.XML
import okhttp3.OkHttpClient
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.core.common.preference.ProfilePreferenceOwnerInstaller
import tachiyomi.core.common.preference.ProfilePreferenceOwnerRegistry
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.entry.interactor.GetEntry
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.interactor.NetworkToLocalEntry
import tachiyomi.domain.entry.interactor.SyncEntryWithSource
import tachiyomi.domain.entry.repository.DownloadPreferencesRepository
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.entry.repository.EntryRepository
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
    private val viewerSettingsScreenProjectionResolver: EntryViewerSettingsScreenProjectionResolver =
        EntryViewerSettingsScreenProjectionResolver { surfaceIds ->
            surfaceIds.map(::viewerSettingsProjection)
        },
) : AutoCloseable {
    private val previousInjekt: InjektScope = Injekt

    init {
        Injekt = InjektScope(DefaultRegistrar())
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    fun composition(): EntryInteractionComposition {
        val application = application()
        val preferenceOwners = ProfilePreferenceOwnerInstaller(
            owners = ProfilePreferenceOwnerRegistry(),
            preferenceStore = ::InMemoryPreferenceStore,
        )
        installControlledRuntimeDependencies(application)
        Injekt.addEntryInteractionRuntime(
            app = application,
            dependencies = EntryInteractionRuntimeDependencies(
                activityTheme = mockk(relaxed = true),
                notificationActions = mockk<EntryDownloadNotificationActions>(relaxed = true),
                pageImageCache = mockk(relaxed = true),
                childGroupFilterDataSource = mockk(relaxed = true),
                mediaSessionIncognitoState = mockk(relaxed = true),
                basePreferenceStore = InMemoryPreferenceStore(),
                profilePreferenceOwners = preferenceOwners,
                viewerSettingsScreenProjectionResolver = viewerSettingsScreenProjectionResolver,
                sourceRefreshUpdateLibraryTitles = { false },
                libraryMembershipHost = mockk<EntryLibraryMembershipHost>(relaxed = true),
                libraryCustomCoverHost = mockk<EntryLibraryCustomCoverHost>(relaxed = true),
                destructiveRemovalHost = mockk<EntryDestructiveRemovalHost>(relaxed = true),
                destructiveRemovalCustomCoverHost = mockk<EntryDestructiveRemovalCustomCoverHost>(relaxed = true),
                profileMoveHost = mockk<EntryProfileMoveHost>(relaxed = true),
                profileMoveSourceVisibilityHost = mockk<EntryProfileMoveSourceVisibilityHost>(relaxed = true),
                profileMoveCustomCoverHost = mockk<EntryProfileMoveCustomCoverHost>(relaxed = true),
                profileMoveTrackingStateHost = mockk<EntryProfileMoveTrackingStateHost>(relaxed = true),
                profileMoveChildGroupFilterStateHost = mockk<EntryProfileMoveChildGroupFilterStateHost>(relaxed = true),
                profileMoveCoverHashStateHost = mockk<EntryProfileMoveCoverHashStateHost>(relaxed = true),
                mergeHost = mockk<EntryMergeHost>(relaxed = true),
                mergeCoverCleanup = {},
                migrationPreparationHost = mockk<EntryMigrationPreparationHost>(relaxed = true),
                migrationExecutionHost = mockk<EntryMigrationExecutionHost>(relaxed = true),
                migrationConsequenceHost = mockk<EntryMigrationConsequenceHost>(relaxed = true),
                migrationCustomCoverHost = mockk<EntryMigrationCustomCoverHost>(relaxed = true),
                trackingHost = mockk<EntryTrackingHost>(relaxed = true),
            ),
        )
        val installation = Injekt.get<EntryFeatureRuntimeInstallation>()
        validateInstalledEntryFeatureRuntimeModules(installation.modules)
        return Injekt.get<EntryInteractionComposition>()
    }

    override fun close() {
        Dispatchers.resetMain()
        Injekt = previousInjekt
    }

    private fun application(): Application {
        return mockk<Application>(relaxed = true).also { application ->
            every { application.cacheDir } returns temporaryDirectory.resolve("cache").also(File::mkdirs)
            every { application.filesDir } returns temporaryDirectory.resolve("files").also(File::mkdirs)
        }
    }

    private fun installControlledRuntimeDependencies(application: Application) {
        val storageManager = mockk<StorageManager>(relaxed = true) {
            every { changes } returns MutableSharedFlow()
            every { getDownloadsDirectory() } returns null
        }
        val networkHelper = mockk<NetworkHelper>(relaxed = true) {
            every { client } returns mockk<OkHttpClient>(relaxed = true)
        }
        Injekt.addSingletonFactory<Application> { application }
        Injekt.addSingletonFactory<StorageManager> { storageManager }
        Injekt.addSingletonFactory<NetworkHelper> { networkHelper }
        Injekt.addSingletonFactory<Json> { Json }
        Injekt.addSingletonFactory<XML> { XML.v1 {} }
        Injekt.addSingletonFactory<GlobalLibraryPreferences> { mockk(relaxed = true) }
        Injekt.addSingletonFactory<LibraryPreferences> { mockk(relaxed = true) }
        Injekt.addSingletonFactory<SourceManager> { mockk(relaxed = true) }
        Injekt.addSingletonFactory<EntryDownloadWorkController> { mockk(relaxed = true) }
        Injekt.addSingletonFactory<EntryPageImageCache> { mockk(relaxed = true) }
        Injekt.addSingletonFactory<EntryMediaSessionIncognitoState> { mockk(relaxed = true) }
        Injekt.addSingletonFactory<EntryDownloadLifecycleEventSink> { mockk(relaxed = true) }
        Injekt.addSingletonFactory<EntryInteractionPreferences> { mockk(relaxed = true) }
        Injekt.addSingletonFactory<ViewerSettingOverrideRepository> { mockk(relaxed = true) }
        Injekt.addSingletonFactory<GetEntryWithChapters> { mockk(relaxed = true) }
        Injekt.addSingletonFactory<GetEntry> { mockk(relaxed = true) }
        Injekt.addSingletonFactory<NetworkToLocalEntry> { mockk(relaxed = true) }
        Injekt.addSingletonFactory<SyncEntryWithSource> { mockk(relaxed = true) }
        Injekt.addSingletonFactory<EntryChapterRepository> { mockk(relaxed = true) }
        Injekt.addSingletonFactory<EntryProgressRepository> { mockk(relaxed = true) }
        Injekt.addSingletonFactory<EntryRepository> { mockk(relaxed = true) }
        Injekt.addSingletonFactory<DownloadPreferences> { mockk(relaxed = true) }
        Injekt.addSingletonFactory<DownloadPreferencesRepository> { mockk(relaxed = true) }
        Injekt.addSingletonFactory<PlaybackPreferencesRepository> { mockk(relaxed = true) }
        Injekt.addSingletonFactory<HistoryRepository> { mockk(relaxed = true) }
        Injekt.addSingletonFactory<GetCategories> { mockk(relaxed = true) }
        Injekt.addSingletonFactory<GetTracks> { mockk(relaxed = true) }
    }
}

private fun viewerSettingsProjection(id: String): EntryViewerSettingsScreenProjection {
    return object : EntryViewerSettingsScreenProjection {
        override val surfaceId = id
    }
}

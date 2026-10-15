package mihon.entry.interactions.download

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.plus
import mihon.core.metro.AppCoroutineScope
import mihon.entry.interactions.download.configuration.backup.DefaultEntryDownloadConfigurationBackupFeature
import mihon.entry.interactions.download.configuration.backup.EntryDownloadConfigurationBackupFeature
import mihon.entry.interactions.download.configuration.backup.EntryDownloadConfigurationBackupParticipant
import mihon.entry.interactions.download.host.EntryDownloadSourceAccessResolver
import mihon.entry.interactions.download.host.SourceManagerEntryDownloadSourceAccessResolver
import mihon.entry.interactions.download.maintenance.merge.EntryDownloadMergeParticipant
import mihon.entry.interactions.download.maintenance.migration.EntryDownloadMigrationParticipant
import mihon.entry.interactions.download.notification.AndroidEntryDownloadNotifier
import mihon.entry.interactions.durable.EntryDurableParticipant
import mihon.entry.interactions.merge.EntryMergeDownloadOwnershipProjection
import mihon.entry.interactions.merge.consequence.EntryMergeDurableEvent
import mihon.entry.interactions.migration.consequence.EntryMigrationDurableEvent
import mihon.entry.interactions.persistence.backup.EntryBackupStateParticipant
import mihon.entry.interactions.runtime.EntryInteractions
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.runtime.FeatureRuntimeComposition
import mihon.feature.runtime.FeatureRuntimeWarmup
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.repository.DownloadPreferencesRepository
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.library.service.GlobalLibraryPreferences
import tachiyomi.domain.source.service.SourceManager

@ContributesTo(AppScope::class)
@BindingContainer
object EntryDownloadBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun workController(application: Application): EntryDownloadWorkController =
        DefaultEntryDownloadWorkController(application)

    @Provides
    @SingleIn(AppScope::class)
    internal fun runtimeCoordinator(
        composition: FeatureRuntimeComposition,
        interactions: EntryInteractions,
        @AppCoroutineScope scope: CoroutineScope,
    ): EntryDownloadRuntimeCoordinator = DefaultEntryDownloadRuntimeFeature(
        evaluation = composition.evaluation,
        interaction = interactions.download,
        scope = scope + Dispatchers.Default,
    )

    @Provides
    internal fun runtimeFeature(coordinator: EntryDownloadRuntimeCoordinator): EntryDownloadRuntimeFeature =
        coordinator

    @Provides
    @IntoSet
    fun runtimeGraphContributor(): FeatureGraphContributor = EntryDownloadRuntimeFeatureContributor

    @Provides
    @SingleIn(AppScope::class)
    internal fun sourceAccessResolver(sourceManager: SourceManager): EntryDownloadSourceAccessResolver =
        SourceManagerEntryDownloadSourceAccessResolver(sourceManager)

    @Provides
    @SingleIn(AppScope::class)
    internal fun actionFeature(
        composition: FeatureRuntimeComposition,
        interactions: EntryInteractions,
        sourceAccessResolver: EntryDownloadSourceAccessResolver,
    ): EntryDownloadActionFeature = DefaultEntryDownloadActionFeature(
        evaluation = composition.evaluation,
        interaction = interactions.download,
        sourceAccessResolver = sourceAccessResolver,
    )

    @Provides
    @SingleIn(AppScope::class)
    internal fun notificationManager(
        application: Application,
        downloads: EntryDownloadRuntimeCoordinator,
        actions: EntryDownloadNotificationActions,
        ownership: EntryMergeDownloadOwnershipProjection,
        securityPreferences: () -> SecurityPreferences,
        @AppCoroutineScope scope: CoroutineScope,
    ): EntryDownloadNotificationManager = EntryDownloadNotificationManager(
        context = application,
        downloads = downloads,
        actions = actions,
        ownership = ownership,
        presenter = AndroidEntryDownloadNotifier(
            context = application,
            actions = actions,
            hideNotificationContent = { securityPreferences().hideNotificationContent.get() },
        ),
        scope = scope,
    )

    @Provides
    internal fun workerDependencies(
        downloads: EntryDownloadRuntimeCoordinator,
        downloadPreferences: DownloadPreferences,
        notificationProvider: EntryDownloadForegroundNotificationProvider,
    ): EntryDownloadWorkerDependencies = EntryDownloadWorkerDependencies(
        downloads = downloads,
        downloadPreferences = downloadPreferences,
        notificationProvider = notificationProvider,
    )

    @Provides
    internal fun foregroundNotificationProvider(
        manager: EntryDownloadNotificationManager,
    ): EntryDownloadForegroundNotificationProvider = manager

    @Provides
    @IntoSet
    fun actionGraphContributor(): FeatureGraphContributor = EntryDownloadActionFeatureContributor

    @Provides
    @IntoSet
    internal fun notificationManagerWarmup(manager: () -> EntryDownloadNotificationManager): FeatureRuntimeWarmup =
        FeatureRuntimeWarmup { manager().start() }

    @Provides
    @SingleIn(AppScope::class)
    internal fun automaticDownloadPolicy(
        entryChapterRepository: EntryChapterRepository,
        downloadPreferences: DownloadPreferences,
        getCategories: GetCategories,
    ): EntryAutomaticDownloadPolicy = EntryAutomaticDownloadPolicy(
        entryChapterRepository = entryChapterRepository,
        downloadPreferences = downloadPreferences,
        getCategories = getCategories,
    )

    @Provides
    @SingleIn(AppScope::class)
    internal fun automaticDownloadCoordinator(
        composition: FeatureRuntimeComposition,
        interactions: EntryInteractions,
        sharedPolicy: EntryAutomaticDownloadPolicy,
    ): EntryAutomaticDownloadCoordinator = DefaultEntryAutomaticDownloadFeature(
        evaluation = composition.evaluation,
        interaction = interactions.download,
        sharedPolicy = sharedPolicy,
    )

    @Provides
    internal fun automaticDownloadFeature(
        coordinator: EntryAutomaticDownloadCoordinator,
    ): EntryAutomaticDownloadFeature = coordinator

    @Provides
    @IntoSet
    fun automaticDownloadGraphContributor(): FeatureGraphContributor = EntryAutomaticDownloadFeatureContributor

    @Provides
    @SingleIn(AppScope::class)
    fun lifecycleEventSink(feature: () -> EntryDownloadLifecycleFeature): EntryDownloadLifecycleEventSink =
        EntryDownloadLifecycleEventSink { event -> feature().onEvent(event) }

    @Provides
    @SingleIn(AppScope::class)
    fun lifecycleFeature(
        composition: FeatureRuntimeComposition,
        downloadPreferences: DownloadPreferences,
        getCategories: GetCategories,
        getEntryWithChapters: GetEntryWithChapters,
        entryRepository: EntryRepository,
        interactions: EntryInteractions,
        globalLibraryPreferences: GlobalLibraryPreferences,
    ): EntryDownloadLifecycleFeature = DefaultEntryDownloadLifecycleFeature(
        evaluation = composition.evaluation,
        downloadPreferences = downloadPreferences,
        getCategories = getCategories,
        getEntryWithChapters = getEntryWithChapters,
        entryRepository = entryRepository,
        downloads = interactions.download,
        globalLibraryPreferences = globalLibraryPreferences,
    )

    @Provides
    @IntoSet
    fun lifecycleGraphContributor(): FeatureGraphContributor = EntryDownloadLifecycleFeatureContributor

    @Provides
    @SingleIn(AppScope::class)
    fun optionsFeature(
        composition: FeatureRuntimeComposition,
        interactions: EntryInteractions,
    ): EntryDownloadOptionsFeature = DefaultEntryDownloadOptionsFeature(
        evaluation = composition.evaluation,
        interaction = interactions.download,
    )

    @Provides
    @SingleIn(AppScope::class)
    fun settingsFeature(composition: FeatureRuntimeComposition): EntryDownloadSettingsFeature =
        DefaultEntryDownloadSettingsFeature(composition.evaluation)

    @Provides
    @SingleIn(AppScope::class)
    fun configurationBackupFeature(
        composition: FeatureRuntimeComposition,
        repository: DownloadPreferencesRepository,
    ): EntryDownloadConfigurationBackupFeature = DefaultEntryDownloadConfigurationBackupFeature(
        evaluation = composition.evaluation,
        repository = repository,
    )

    @Provides
    @IntoSet
    fun configurationGraphContributor(): FeatureGraphContributor = EntryDownloadConfigurationFeatureContributor

    @Provides
    @SingleIn(AppScope::class)
    internal fun maintenanceFeature(
        composition: FeatureRuntimeComposition,
        interactions: EntryInteractions,
        ownership: EntryMergeDownloadOwnershipProjection,
    ): EntryDownloadMaintenanceFeature = DefaultEntryDownloadMaintenanceFeature(
        evaluation = composition.evaluation,
        interaction = interactions.download,
        ownership = ownership,
    )

    @Provides
    @IntoSet
    fun maintenanceGraphContributor(): FeatureGraphContributor = EntryDownloadMaintenanceFeatureContributor

    @Provides
    @IntoSet
    internal fun migrationDurableParticipant(
        feature: () -> EntryDownloadMaintenanceFeature,
    ): EntryDurableParticipant<EntryMigrationDurableEvent> = EntryDownloadMigrationParticipant(feature)

    @Provides
    @IntoSet
    internal fun mergeDurableParticipant(
        feature: () -> EntryDownloadMaintenanceFeature,
    ): EntryDurableParticipant<EntryMergeDurableEvent> = EntryDownloadMergeParticipant(feature)

    @Provides
    @IntoSet
    internal fun configurationBackupState(
        feature: () -> EntryDownloadConfigurationBackupFeature,
    ): EntryBackupStateParticipant = EntryDownloadConfigurationBackupParticipant(feature)
}

package mihon.entry.interactions.download

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.plus
import mihon.core.metro.AppCoroutineScope
import mihon.entry.interactions.download.configuration.backup.DefaultEntryDownloadConfigurationBackupFeature
import mihon.entry.interactions.download.configuration.backup.ENTRY_DOWNLOAD_CONFIGURATION_BACKUP_RESTORE_PARTICIPANT
import mihon.entry.interactions.download.configuration.backup.ENTRY_DOWNLOAD_CONFIGURATION_BACKUP_SCHEMA_VERSION
import mihon.entry.interactions.download.configuration.backup.ENTRY_DOWNLOAD_CONFIGURATION_BACKUP_SNAPSHOT_PARTICIPANT
import mihon.entry.interactions.download.configuration.backup.ENTRY_DOWNLOAD_CONFIGURATION_BACKUP_STATE_ID
import mihon.entry.interactions.download.configuration.backup.EntryDownloadConfigurationBackupContributor
import mihon.entry.interactions.download.configuration.backup.EntryDownloadConfigurationBackupFeature
import mihon.entry.interactions.download.configuration.backup.EntryDownloadConfigurationBackupState
import mihon.entry.interactions.download.host.EntryDownloadSourceAccessResolver
import mihon.entry.interactions.download.host.SourceManagerEntryDownloadSourceAccessResolver
import mihon.entry.interactions.download.maintenance.ENTRY_DOWNLOAD_DESTRUCTIVE_REMOVAL_PARTICIPANT
import mihon.entry.interactions.download.maintenance.ENTRY_DOWNLOAD_DESTRUCTIVE_REMOVAL_PREPARATION_PARTICIPANT
import mihon.entry.interactions.download.maintenance.ENTRY_DOWNLOAD_METADATA_CHANGE_PARTICIPANT
import mihon.entry.interactions.download.maintenance.ENTRY_DOWNLOAD_PROFILE_MOVE_PARTICIPANT
import mihon.entry.interactions.download.maintenance.ENTRY_DOWNLOAD_PROFILE_MOVE_PREPARATION_PARTICIPANT
import mihon.entry.interactions.download.maintenance.EntryDownloadDestructiveRemovalContributor
import mihon.entry.interactions.download.maintenance.EntryDownloadMetadataLifecycleContributor
import mihon.entry.interactions.download.maintenance.EntryDownloadProfileMoveContributor
import mihon.entry.interactions.download.maintenance.merge.EntryDownloadMergeContributor
import mihon.entry.interactions.download.maintenance.merge.entryDownloadMergeBinding
import mihon.entry.interactions.download.maintenance.migration.EntryDownloadMigrationContributor
import mihon.entry.interactions.download.maintenance.migration.entryDownloadMigrationBinding
import mihon.entry.interactions.download.maintenance.migration.entryDownloadMigrationOptionBinding
import mihon.entry.interactions.media.session.EntryMediaSessionEvent
import mihon.entry.interactions.merge.EntryMergeDownloadOwnershipProjection
import mihon.entry.interactions.persistence.backup.decodeEntryBackupState
import mihon.entry.interactions.persistence.backup.entryBackupStateEnvelope
import mihon.entry.interactions.runtime.EntryInteractions
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.execution.FeatureDurableExecutionParticipantBinding
import mihon.feature.graph.execution.FeatureExecutionHandler
import mihon.feature.graph.execution.FeatureExecutionParticipantBinding
import mihon.feature.runtime.FeatureRuntimeComposition
import mihon.feature.runtime.FeatureRuntimeWarmup
import mihon.feature.runtime.injekt.InjektExport
import mihon.feature.runtime.injekt.injektExport
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
    @IntoSet
    fun exportWorkController(controller: () -> EntryDownloadWorkController): InjektExport<*> =
        injektExport(controller)

    @Provides
    @IntoSet
    internal fun exportRuntimeCoordinator(coordinator: () -> EntryDownloadRuntimeCoordinator): InjektExport<*> =
        injektExport(coordinator)

    @Provides
    @IntoSet
    fun exportRuntimeFeature(feature: () -> EntryDownloadRuntimeFeature): InjektExport<*> = injektExport(feature)

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
        @AppCoroutineScope scope: CoroutineScope,
    ): EntryDownloadNotificationManager = EntryDownloadNotificationManager(
        context = application,
        downloads = downloads,
        actions = actions,
        ownership = ownership,
        scope = scope,
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
    @IntoSet
    internal fun exportSourceAccessResolver(resolver: () -> EntryDownloadSourceAccessResolver): InjektExport<*> =
        injektExport(resolver)

    @Provides
    @IntoSet
    fun exportActionFeature(feature: () -> EntryDownloadActionFeature): InjektExport<*> = injektExport(feature)

    @Provides
    @IntoSet
    internal fun exportNotificationManager(manager: () -> EntryDownloadNotificationManager): InjektExport<*> =
        injektExport(manager)

    @Provides
    @IntoSet
    fun exportForegroundNotificationProvider(
        provider: () -> EntryDownloadForegroundNotificationProvider,
    ): InjektExport<*> = injektExport(provider)

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
    @IntoSet
    fun automaticDownloadRefreshGraphContributor(): FeatureGraphContributor =
        EntryAutomaticDownloadRefreshContributor

    @Provides
    @IntoSet
    internal fun automaticDownloadSourceRefreshParticipant(
        coordinator: () -> EntryAutomaticDownloadCoordinator,
    ): FeatureExecutionParticipantBinding<*> = entryAutomaticDownloadSourceRefreshBinding(coordinator)

    @Provides
    @IntoSet
    internal fun automaticDownloadLibraryUpdateParticipant(
        coordinator: () -> EntryAutomaticDownloadCoordinator,
    ): FeatureExecutionParticipantBinding<*> = entryAutomaticDownloadLibraryUpdateBinding(coordinator)

    @Provides
    @IntoSet
    internal fun exportAutomaticDownloadPolicy(policy: () -> EntryAutomaticDownloadPolicy): InjektExport<*> =
        injektExport(policy)

    @Provides
    @IntoSet
    internal fun exportAutomaticDownloadCoordinator(
        coordinator: () -> EntryAutomaticDownloadCoordinator,
    ): InjektExport<*> = injektExport(coordinator)

    @Provides
    @IntoSet
    fun exportAutomaticDownloadFeature(feature: () -> EntryAutomaticDownloadFeature): InjektExport<*> =
        injektExport(feature)

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
    @IntoSet
    fun mediaSessionGraphContributor(): FeatureGraphContributor = EntryDownloadMediaSessionContributor

    @Provides
    @IntoSet
    fun mediaSessionParticipant(
        feature: () -> EntryDownloadLifecycleFeature,
    ): FeatureExecutionParticipantBinding<*> = entryDownloadMediaSessionBinding(feature)

    @Provides
    @IntoSet
    fun exportLifecycleEventSink(sink: () -> EntryDownloadLifecycleEventSink): InjektExport<*> = injektExport(sink)

    @Provides
    @IntoSet
    fun exportLifecycleFeature(feature: () -> EntryDownloadLifecycleFeature): InjektExport<*> = injektExport(feature)

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
    @IntoSet
    fun configurationBackupGraphContributor(): FeatureGraphContributor = EntryDownloadConfigurationBackupContributor

    @Provides
    @IntoSet
    fun configurationBackupSnapshotParticipant(
        feature: () -> EntryDownloadConfigurationBackupFeature,
    ): FeatureExecutionParticipantBinding<*> = FeatureExecutionParticipantBinding(
        definition = ENTRY_DOWNLOAD_CONFIGURATION_BACKUP_SNAPSHOT_PARTICIPANT,
        handler = FeatureExecutionHandler { event ->
            if (!event.selection.includeContentState) return@FeatureExecutionHandler
            val state = feature().snapshot(event.entry)
                ?: return@FeatureExecutionHandler
            event.contributions.add(
                entryBackupStateEnvelope(
                    ENTRY_DOWNLOAD_CONFIGURATION_BACKUP_STATE_ID,
                    ENTRY_DOWNLOAD_CONFIGURATION_BACKUP_SCHEMA_VERSION,
                    EntryDownloadConfigurationBackupState.serializer(),
                    state,
                ),
            )
        },
    )

    @Provides
    @IntoSet
    fun configurationBackupRestoreParticipant(
        feature: () -> EntryDownloadConfigurationBackupFeature,
    ): FeatureExecutionParticipantBinding<*> = FeatureExecutionParticipantBinding(
        definition = ENTRY_DOWNLOAD_CONFIGURATION_BACKUP_RESTORE_PARTICIPANT,
        handler = FeatureExecutionHandler { event ->
            val state = event.states.decodeEntryBackupState(
                ENTRY_DOWNLOAD_CONFIGURATION_BACKUP_STATE_ID,
                ENTRY_DOWNLOAD_CONFIGURATION_BACKUP_SCHEMA_VERSION,
                EntryDownloadConfigurationBackupState.serializer(),
            ) ?: return@FeatureExecutionHandler
            feature().restore(event.entry, state)
        },
    )

    @Provides
    @IntoSet
    fun exportOptionsFeature(feature: () -> EntryDownloadOptionsFeature): InjektExport<*> = injektExport(feature)

    @Provides
    @IntoSet
    fun exportSettingsFeature(feature: () -> EntryDownloadSettingsFeature): InjektExport<*> = injektExport(feature)

    @Provides
    @IntoSet
    fun exportConfigurationBackupFeature(
        feature: () -> EntryDownloadConfigurationBackupFeature,
    ): InjektExport<*> = injektExport(feature)

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
    fun libraryMembershipGraphContributor(): FeatureGraphContributor = EntryDownloadLibraryMembershipContributor

    @Provides
    @IntoSet
    fun metadataLifecycleGraphContributor(): FeatureGraphContributor = EntryDownloadMetadataLifecycleContributor

    @Provides
    @IntoSet
    fun destructiveRemovalGraphContributor(): FeatureGraphContributor = EntryDownloadDestructiveRemovalContributor

    @Provides
    @IntoSet
    fun profileMoveGraphContributor(): FeatureGraphContributor = EntryDownloadProfileMoveContributor

    @Provides
    @IntoSet
    fun migrationGraphContributor(): FeatureGraphContributor = EntryDownloadMigrationContributor

    @Provides
    @IntoSet
    fun mergeGraphContributor(): FeatureGraphContributor = EntryDownloadMergeContributor

    @Provides
    @IntoSet
    fun migrationDurableParticipant(
        feature: () -> EntryDownloadMaintenanceFeature,
    ): FeatureDurableExecutionParticipantBinding<*> = entryDownloadMigrationBinding(feature)

    @Provides
    @IntoSet
    fun mergeDurableParticipant(
        feature: () -> EntryDownloadMaintenanceFeature,
    ): FeatureDurableExecutionParticipantBinding<*> = entryDownloadMergeBinding(feature)

    @Provides
    @IntoSet
    fun migrationOptionParticipant(
        feature: () -> EntryDownloadMaintenanceFeature,
    ): FeatureExecutionParticipantBinding<*> = entryDownloadMigrationOptionBinding(feature)

    @Provides
    @IntoSet
    fun libraryRemovalParticipant(
        feature: () -> EntryDownloadMaintenanceFeature,
    ): FeatureExecutionParticipantBinding<*> = FeatureExecutionParticipantBinding(
        definition = ENTRY_DOWNLOAD_LIBRARY_REMOVAL_PARTICIPANT,
        handler = FeatureExecutionHandler { event ->
            event.entries.forEach { entry ->
                if (feature().inspectEntry(entry) == EntryDownloadMaintenanceInspection.HasDownloads) {
                    event.outcomes.requireDownloadDecision(entry)
                }
            }
        },
    )

    @Provides
    @IntoSet
    fun metadataChangeParticipant(
        feature: () -> EntryDownloadMaintenanceFeature,
    ): FeatureExecutionParticipantBinding<*> = FeatureExecutionParticipantBinding(
        definition = ENTRY_DOWNLOAD_METADATA_CHANGE_PARTICIPANT,
        handler = FeatureExecutionHandler { event ->
            if (event.previous.title != event.current.title) {
                feature().renameEntry(
                    event.previous,
                    event.current.title,
                )
            }
        },
    )

    @Provides
    @IntoSet
    fun destructiveRemovalPreparationParticipant(
        feature: () -> EntryDownloadMaintenanceFeature,
    ): FeatureExecutionParticipantBinding<*> = FeatureExecutionParticipantBinding(
        definition = ENTRY_DOWNLOAD_DESTRUCTIVE_REMOVAL_PREPARATION_PARTICIPANT,
        handler = FeatureExecutionHandler { event ->
            event.entries.forEach { entry ->
                when (val preparation = feature().prepareRemoval(entry)) {
                    is EntryDownloadRemovalPreparation.Prepared -> event.outcomes.addDownloadPlan(
                        preparation.plan,
                    )

                    EntryDownloadRemovalPreparation.NothingToRemove,
                    is EntryDownloadRemovalPreparation.Inapplicable,
                    -> Unit
                }
            }
        },
    )

    @Provides
    @IntoSet
    fun destructiveRemovalParticipant(
        feature: () -> EntryDownloadMaintenanceFeature,
    ): FeatureExecutionParticipantBinding<*> = FeatureExecutionParticipantBinding(
        definition = ENTRY_DOWNLOAD_DESTRUCTIVE_REMOVAL_PARTICIPANT,
        handler = FeatureExecutionHandler { event ->
            val type = event.entries.firstOrNull()?.type ?: return@FeatureExecutionHandler
            val owners = event.outcomes.downloadPlans
                .flatMap(EntryDownloadRemovalPlan::owners)
                .filter { owner -> owner.type == type }
                .distinctBy { owner -> owner.profileId to owner.id }
            if (owners.isNotEmpty()) {
                check(
                    feature().applyRemoval(EntryDownloadRemovalPlan(owners)) ==
                        EntryDownloadMaintenanceResult.Performed,
                ) { "Download cleanup was incomplete after destructive Entry removal" }
            }
        },
    )

    @Provides
    @IntoSet
    fun profileMovePreparationParticipant(
        feature: () -> EntryDownloadMaintenanceFeature,
    ): FeatureExecutionParticipantBinding<*> = FeatureExecutionParticipantBinding(
        definition = ENTRY_DOWNLOAD_PROFILE_MOVE_PREPARATION_PARTICIPANT,
        handler = FeatureExecutionHandler { event ->
            event.plan.removedEntries.forEach { entry ->
                when (val preparation = feature().prepareRemoval(entry)) {
                    is EntryDownloadRemovalPreparation.Prepared -> event.outcomes.addDownloadPlan(
                        preparation.plan,
                    )

                    EntryDownloadRemovalPreparation.NothingToRemove,
                    is EntryDownloadRemovalPreparation.Inapplicable,
                    -> Unit
                }
            }
        },
    )

    @Provides
    @IntoSet
    fun profileMoveParticipant(
        feature: () -> EntryDownloadMaintenanceFeature,
    ): FeatureExecutionParticipantBinding<*> = FeatureExecutionParticipantBinding(
        definition = ENTRY_DOWNLOAD_PROFILE_MOVE_PARTICIPANT,
        handler = FeatureExecutionHandler { event ->
            val owners = event.outcomes.downloadPlans
                .flatMap(EntryDownloadRemovalPlan::owners)
                .filter { owner -> owner.type == event.type }
                .distinctBy { owner -> owner.profileId to owner.id }
            if (owners.isNotEmpty()) {
                check(
                    feature().applyRemoval(EntryDownloadRemovalPlan(owners)) ==
                        EntryDownloadMaintenanceResult.Performed,
                ) { "Download cleanup was incomplete after Profile movement" }
            }
        },
    )

    @Provides
    @IntoSet
    fun exportMaintenanceFeature(feature: () -> EntryDownloadMaintenanceFeature): InjektExport<*> =
        injektExport(feature)
}

internal fun entryAutomaticDownloadSourceRefreshBinding(
    feature: () -> EntryAutomaticDownloadCoordinator,
) = FeatureExecutionParticipantBinding(
    definition = ENTRY_AUTOMATIC_DOWNLOAD_SOURCE_REFRESH_PARTICIPANT,
    handler = FeatureExecutionHandler { event ->
        feature().downloadAfterEntryRefresh(event.entry, event.newChildren)
    },
)

internal fun entryAutomaticDownloadLibraryUpdateBinding(
    feature: () -> EntryAutomaticDownloadCoordinator,
) = FeatureExecutionParticipantBinding(
    definition = ENTRY_AUTOMATIC_DOWNLOAD_LIBRARY_UPDATE_PARTICIPANT,
    handler = FeatureExecutionHandler { event ->
        val batch = event.session.state(
            participant = ENTRY_AUTOMATIC_DOWNLOAD_LIBRARY_UPDATE_PARTICIPANT.id,
            create = feature()::newLibraryUpdateBatch,
            complete = EntryAutomaticDownloadBatch::complete,
        )
        batch.enqueue(event.entry, event.newChildren)
    },
)

internal fun entryDownloadMediaSessionBinding(
    feature: () -> EntryDownloadLifecycleFeature,
) = FeatureExecutionParticipantBinding(
    definition = ENTRY_DOWNLOAD_MEDIA_SESSION_PARTICIPANT,
    handler = FeatureExecutionHandler { execution ->
        val event = execution.event as? EntryMediaSessionEvent.Progressed
            ?: return@FeatureExecutionHandler
        event.fraction?.let { fraction ->
            feature().onEvent(
                EntryDownloadLifecycleEvent.Progressed(
                    visibleEntry = event.visibleEntry,
                    child = event.child,
                    fraction = fraction,
                    deduplicateByNumber = event.deduplicateDownloadByNumber,
                ),
            )
        }
        if (execution.progressResult?.completedNow == true) {
            feature().onEvent(
                EntryDownloadLifecycleEvent.Completed(
                    visibleEntry = event.visibleEntry,
                    child = event.child,
                    deduplicateByNumber = event.deduplicateDownloadByNumber,
                    completeEquivalentChildrenByNumber = event.completeEquivalentChildrenByNumber,
                ),
            )
        }
    },
)

package mihon.entry.interactions.library

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.download.EntryAutomaticDownloadCoordinator
import mihon.entry.interactions.download.EntryDownloadActionFeature
import mihon.entry.interactions.download.EntryDownloadMaintenanceFeature
import mihon.entry.interactions.library.membership.EntryLibraryMembershipCoordinator
import mihon.entry.interactions.library.membership.EntryLibraryMembershipFeature
import mihon.entry.interactions.library.membership.host.EntryLibraryCustomCoverHost
import mihon.entry.interactions.library.membership.host.EntryLibraryMembershipHost
import mihon.entry.interactions.merge.EntryMergeCandidateFeature
import mihon.entry.interactions.merge.EntryMergeLibraryLifecycleFeature
import mihon.entry.interactions.merge.EntryMergeNavigationFeature
import mihon.entry.interactions.merge.EntryMergeSubject
import mihon.entry.interactions.navigation.EntryContinueFeature
import mihon.entry.interactions.navigation.EntryContinueProcessor
import mihon.entry.interactions.navigation.EntryOpenFeature
import mihon.entry.interactions.presentation.EntryTypePresentationFeature
import mihon.entry.interactions.runtime.EntryInteractionPlugin
import mihon.entry.interactions.runtime.EntryInteractions
import mihon.entry.interactions.runtime.EntryOutsideReleasePeriodFilterProvider
import mihon.entry.interactions.source.EntrySourceRefreshFeature
import mihon.entry.interactions.state.EntryBookmarkProcessor
import mihon.entry.interactions.state.EntryConsumptionFeature
import mihon.entry.interactions.tracking.EntryTrackingFeature
import mihon.feature.graph.FeatureGraphContributor
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.service.EntryLibraryProgressResolutionPort
import tachiyomi.domain.source.service.SourceManager

@ContributesTo(AppScope::class)
@BindingContainer
object EntryLibraryBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun membershipFeature(
        host: EntryLibraryMembershipHost,
        mergeCandidates: EntryMergeCandidateFeature,
        mergeLibraryLifecycle: () -> EntryMergeLibraryLifecycleFeature,
        tracking: () -> EntryTrackingFeature,
        downloads: () -> EntryDownloadMaintenanceFeature,
        customCoverHost: EntryLibraryCustomCoverHost,
    ): EntryLibraryMembershipFeature = EntryLibraryMembershipCoordinator(
        host = host,
        mergeCandidates = mergeCandidates,
        mergeLibraryLifecycle = mergeLibraryLifecycle,
        tracking = tracking,
        downloads = downloads,
        customCoverHost = customCoverHost,
    )

    @Provides
    @SingleIn(AppScope::class)
    fun filterFeature(
        libraryProgressProviders: Map<EntryType, EntryLibraryProgressProvider>,
        bookmarkProcessors: Map<EntryType, EntryBookmarkProcessor>,
        outsideReleasePeriodFilterProviders: Map<EntryType, EntryOutsideReleasePeriodFilterProvider>,
    ): EntryLibraryFilterFeature = DefaultEntryLibraryFilterFeature(
        EntryLibraryFilterTypes(
            progress = libraryProgressProviders.keys,
            bookmark = libraryProgressProviders.keys intersect bookmarkProcessors.keys,
            releasePeriod = outsideReleasePeriodFilterProviders.keys,
        ),
    )

    @Provides
    @SingleIn(AppScope::class)
    fun progressFeature(
        libraryProgressProviders: Map<EntryType, EntryLibraryProgressProvider>,
        continueProcessors: Map<EntryType, EntryContinueProcessor>,
        bookmarkProcessors: Map<EntryType, EntryBookmarkProcessor>,
        interactions: EntryInteractions,
        continueFeature: EntryContinueFeature,
        entryProgressRepository: EntryProgressRepository,
    ): EntryLibraryProgressFeature = DefaultEntryLibraryProgressFeature(
        types = EntryLibraryProgressTypes(
            applicable = libraryProgressProviders.keys,
            continueTarget = libraryProgressProviders.keys intersect continueProcessors.keys,
            bookmark = libraryProgressProviders.keys intersect bookmarkProcessors.keys,
        ),
        interaction = interactions.libraryProgress,
        continueFeature = continueFeature,
        entryProgressRepository = entryProgressRepository,
    )

    @Provides
    fun progressResolutionPort(feature: EntryLibraryProgressFeature): EntryLibraryProgressResolutionPort = feature

    @Provides
    @SingleIn(AppScope::class)
    internal fun updateRefreshFeature(
        sourceRefresh: EntrySourceRefreshFeature,
        automaticDownloads: () -> EntryAutomaticDownloadCoordinator,
    ): EntryLibraryUpdateRefreshFeature = DefaultEntryLibraryUpdateRefreshFeature(
        sourceRefresh = sourceRefresh,
        automaticDownloads = automaticDownloads,
    )

    @Provides
    @SingleIn(AppScope::class)
    fun updateNotificationFeature(
        plugins: Set<EntryInteractionPlugin>,
        presentationFeature: EntryTypePresentationFeature,
        openFeature: EntryOpenFeature,
        consumptionFeature: EntryConsumptionFeature,
        downloadActionFeature: EntryDownloadActionFeature,
        sourceManager: SourceManager,
        mergeNavigation: () -> EntryMergeNavigationFeature,
        entryRepository: () -> EntryRepository,
    ): EntryLibraryUpdateNotificationFeature = DefaultEntryLibraryUpdateNotificationFeature(
        participatingTypes = plugins.mapTo(mutableSetOf(), EntryInteractionPlugin::type),
        presentationFeature = presentationFeature,
        openFeature = openFeature,
        consumptionFeature = consumptionFeature,
        downloadActionFeature = downloadActionFeature,
        sourceManager = sourceManager,
        resolveVisibleEntry = { entry ->
            val visibleEntryId = mergeNavigation()
                .resolveNavigation(EntryMergeSubject(entry.profileId, entry.id))
                .visibleEntryId
            entryRepository().getEntryById(visibleEntryId, entry.profileId) ?: entry
        },
    )

    @Provides
    @IntoSet
    fun filterGraphContributor(): FeatureGraphContributor = EntryLibraryFilterFeatureContributor

    @Provides
    @IntoSet
    fun progressGraphContributor(): FeatureGraphContributor = EntryLibraryProgressFeatureContributor

    @Provides
    @IntoSet
    fun updateRefreshGraphContributor(): FeatureGraphContributor = EntryLibraryUpdateRefreshFeatureContributor

    @Provides
    @IntoSet
    fun updateNotificationGraphContributor(): FeatureGraphContributor =
        EntryLibraryUpdateNotificationFeatureContributor
}

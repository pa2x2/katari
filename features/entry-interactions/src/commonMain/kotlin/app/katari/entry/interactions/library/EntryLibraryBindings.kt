package app.katari.entry.interactions.library

import app.katari.entry.interactions.download.EntryAutomaticDownloadCoordinator
import app.katari.entry.interactions.download.EntryDownloadActionFeature
import app.katari.entry.interactions.download.EntryDownloadMaintenanceFeature
import app.katari.entry.interactions.library.membership.EntryLibraryMembershipCoordinator
import app.katari.entry.interactions.library.membership.EntryLibraryMembershipFeature
import app.katari.entry.interactions.library.membership.host.EntryLibraryCustomCoverHost
import app.katari.entry.interactions.library.membership.host.EntryLibraryMembershipHost
import app.katari.entry.interactions.merge.EntryMergeCandidateFeature
import app.katari.entry.interactions.merge.EntryMergeLibraryLifecycleFeature
import app.katari.entry.interactions.merge.EntryMergeNavigationFeature
import app.katari.entry.interactions.merge.EntryMergeSubject
import app.katari.entry.interactions.navigation.EntryContinueFeature
import app.katari.entry.interactions.navigation.EntryContinueProcessor
import app.katari.entry.interactions.navigation.EntryOpenFeature
import app.katari.entry.interactions.presentation.EntryTypePresentationFeature
import app.katari.entry.interactions.runtime.EntryInteractions
import app.katari.entry.interactions.runtime.EntryOutsideReleasePeriodFilterProvider
import app.katari.entry.interactions.runtime.EntryTypePresentationProvider
import app.katari.entry.interactions.source.EntrySourceRefreshFeature
import app.katari.entry.interactions.state.EntryBookmarkProcessor
import app.katari.entry.interactions.state.EntryConsumptionFeature
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.source.entry.EntryType
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
        downloads: () -> EntryDownloadMaintenanceFeature,
        customCoverHost: EntryLibraryCustomCoverHost,
    ): EntryLibraryMembershipFeature = EntryLibraryMembershipCoordinator(
        host = host,
        mergeCandidates = mergeCandidates,
        mergeLibraryLifecycle = mergeLibraryLifecycle,
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
        typePresentationProviders: Map<EntryType, EntryTypePresentationProvider>,
        presentationFeature: EntryTypePresentationFeature,
        openFeature: EntryOpenFeature,
        consumptionFeature: EntryConsumptionFeature,
        downloadActionFeature: EntryDownloadActionFeature,
        sourceManager: SourceManager,
        mergeNavigation: () -> EntryMergeNavigationFeature,
        entryRepository: () -> EntryRepository,
    ): EntryLibraryUpdateNotificationFeature = DefaultEntryLibraryUpdateNotificationFeature(
        participatingTypes = typePresentationProviders.keys,
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
}

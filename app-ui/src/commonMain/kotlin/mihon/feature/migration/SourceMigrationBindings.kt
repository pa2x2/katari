package mihon.feature.migration

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import mihon.entry.interactions.catalogue.EntryCatalogueFeature
import mihon.entry.interactions.merge.EntryMergeLibraryGroupingFeature
import mihon.entry.interactions.migration.EntryMigrationFeature
import mihon.feature.migration.discovery.SourceMigrationCandidateDiscovery
import mihon.feature.migration.discovery.SourceMigrationDiscoveryRunner
import mihon.feature.migration.execution.SourceMigrationExecutionPlanner
import mihon.feature.migration.execution.SourceMigrationExecutionRunner
import mihon.feature.migration.review.SourceMigrationCandidateEntryResolver
import mihon.feature.migration.review.SourceMigrationTargetSelector
import mihon.feature.migration.session.SourceMigrationSessionStore
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.entry.interactor.NetworkToLocalEntry
import tachiyomi.domain.entry.repository.EntryRepository

/** The Source Migration sessions and the steps that move them along, which each platform's background work runs. */
@ContributesTo(AppScope::class)
@BindingContainer
object SourceMigrationBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun sourceMigrationCandidateDiscovery(catalogue: EntryCatalogueFeature): SourceMigrationCandidateDiscovery =
        SourceMigrationCandidateDiscovery(catalogue)

    @Provides
    @SingleIn(AppScope::class)
    fun sourceMigrationDiscoveryRunner(
        store: SourceMigrationSessionStore,
        discovery: SourceMigrationCandidateDiscovery,
        entryRepository: EntryRepository,
        networkToLocalEntry: NetworkToLocalEntry,
        migration: EntryMigrationFeature,
    ): SourceMigrationDiscoveryRunner = SourceMigrationDiscoveryRunner(
        store = store,
        discovery = discovery,
        entryRepository = entryRepository,
        networkToLocalEntry = networkToLocalEntry,
        migration = migration,
    )

    @Provides
    @SingleIn(AppScope::class)
    fun sourceMigrationExecutionPlanner(
        grouping: EntryMergeLibraryGroupingFeature,
        entryRepository: EntryRepository,
    ): SourceMigrationExecutionPlanner = SourceMigrationExecutionPlanner(grouping, entryRepository)

    @Provides
    @SingleIn(AppScope::class)
    fun sourceMigrationExecutionRunner(
        store: SourceMigrationSessionStore,
        entryRepository: EntryRepository,
        migration: EntryMigrationFeature,
    ): SourceMigrationExecutionRunner = SourceMigrationExecutionRunner(store, entryRepository, migration)

    @Provides
    @SingleIn(AppScope::class)
    fun sourceMigrationSessionStore(handler: DatabaseHandler): SourceMigrationSessionStore =
        SourceMigrationSessionStore(handler)

    @Provides
    @SingleIn(AppScope::class)
    internal fun sourceMigrationCandidateEntryResolver(
        entryRepository: EntryRepository,
        networkToLocalEntry: NetworkToLocalEntry,
    ): SourceMigrationCandidateEntryResolver = SourceMigrationCandidateEntryResolver(
        entryRepository,
        networkToLocalEntry,
    )

    @Provides
    @SingleIn(AppScope::class)
    internal fun sourceMigrationTargetSelector(
        sessionStore: SourceMigrationSessionStore,
        candidateEntryResolver: SourceMigrationCandidateEntryResolver,
        migration: EntryMigrationFeature,
    ): SourceMigrationTargetSelector = SourceMigrationTargetSelector(
        sessionStore,
        candidateEntryResolver,
        migration,
    )
}

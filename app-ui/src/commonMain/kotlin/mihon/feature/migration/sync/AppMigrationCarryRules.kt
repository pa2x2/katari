package mihon.feature.migration.sync

import app.katari.sync.client.kind.replacement.carry.MigrationCarryRules
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.migration.EntryMigrationFeature
import mihon.entry.interactions.tracking.EntryTrackingFeature
import mihon.entry.interactions.tracking.EntryTrackingServiceId
import tachiyomi.domain.entry.migration.MigrationChildStateTransfer

/** The app's migration rules that sync carries edits of a migrated entry by. */
@Inject
@ContributesBinding(AppScope::class)
class AppMigrationCarryRules(
    private val migration: EntryMigrationFeature,
    private val tracking: EntryTrackingFeature,
) : MigrationCarryRules {
    override fun chapterMarks(type: EntryType): MigrationChildStateTransfer = migration.childStateTransfer(type)

    override fun copiesTrackerBinding(trackerId: Long): Boolean =
        tracking.copiesBindingOnMigration(EntryTrackingServiceId(trackerId))
}

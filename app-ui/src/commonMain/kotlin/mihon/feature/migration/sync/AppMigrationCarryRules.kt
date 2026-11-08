package mihon.feature.migration.sync

import app.katari.sync.client.kind.replacement.carry.MigrationCarryRules
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.migration.EntryMigrationFeature
import tachiyomi.domain.entry.migration.MigrationChildStateTransfer

/** The app's migration rules that sync carries edits of a migrated entry by. */
@Inject
@ContributesBinding(AppScope::class)
class AppMigrationCarryRules(
    private val migration: EntryMigrationFeature,
) : MigrationCarryRules {
    override fun chapterMarks(type: EntryType): MigrationChildStateTransfer = migration.childStateTransfer(type)
}

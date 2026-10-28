package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import mihon.desktop.migration.NoEntryMigrationFeature
import mihon.entry.interactions.migration.EntryMigrationFeature

@BindingContainer
interface MigrationBindings {
    @Binds
    val NoEntryMigrationFeature.entryMigrationFeature: EntryMigrationFeature
}

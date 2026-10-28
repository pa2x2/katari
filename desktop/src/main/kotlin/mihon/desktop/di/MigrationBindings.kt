package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import mihon.desktop.migration.NoEntryMigrationFeature
import mihon.desktop.migration.NoSourceMigrationReviewVisibility
import mihon.desktop.migration.NoSourceMigrationWorkScheduler
import mihon.entry.interactions.migration.EntryMigrationFeature
import mihon.feature.migration.work.SourceMigrationReviewVisibility
import mihon.feature.migration.work.SourceMigrationWorkScheduler

@BindingContainer
interface MigrationBindings {
    @Binds
    val NoEntryMigrationFeature.entryMigrationFeature: EntryMigrationFeature

    @Binds
    val NoSourceMigrationWorkScheduler.sourceMigrationWorkScheduler: SourceMigrationWorkScheduler

    @Binds
    val NoSourceMigrationReviewVisibility.sourceMigrationReviewVisibility: SourceMigrationReviewVisibility
}

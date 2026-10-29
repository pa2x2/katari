package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import mihon.desktop.migration.NoSourceMigrationReviewVisibility
import mihon.desktop.migration.NoSourceMigrationWorkScheduler
import mihon.feature.migration.work.SourceMigrationReviewVisibility
import mihon.feature.migration.work.SourceMigrationWorkScheduler

@BindingContainer
interface MigrationBindings {
    @Binds
    val NoSourceMigrationWorkScheduler.sourceMigrationWorkScheduler: SourceMigrationWorkScheduler

    @Binds
    val NoSourceMigrationReviewVisibility.sourceMigrationReviewVisibility: SourceMigrationReviewVisibility
}

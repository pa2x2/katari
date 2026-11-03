package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.IntoSet
import mihon.desktop.migration.DesktopSourceMigrationReviewVisibility
import mihon.desktop.migration.InProcessSourceMigrationWork
import mihon.desktop.work.ResumableWork
import mihon.feature.migration.work.SourceMigrationReviewVisibility
import mihon.feature.migration.work.SourceMigrationWork

@BindingContainer
interface MigrationBindings {
    @Binds
    val InProcessSourceMigrationWork.sourceMigrationWork: SourceMigrationWork

    @Binds
    @IntoSet
    val InProcessSourceMigrationWork.resumableSourceMigrationWork: ResumableWork

    @Binds
    val DesktopSourceMigrationReviewVisibility.sourceMigrationReviewVisibility: SourceMigrationReviewVisibility
}

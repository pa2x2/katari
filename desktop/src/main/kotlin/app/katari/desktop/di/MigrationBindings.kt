package app.katari.desktop.di

import app.katari.desktop.migration.DesktopSourceMigrationReviewVisibility
import app.katari.desktop.migration.InProcessSourceMigrationWork
import app.katari.desktop.work.ResumableWork
import app.katari.sourcemigration.work.SourceMigrationReviewVisibility
import app.katari.sourcemigration.work.SourceMigrationWork
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.IntoSet

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

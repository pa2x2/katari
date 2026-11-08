package app.katari.android.work

import androidx.work.ListenableWorker
import app.katari.android.sync.SyncJob
import app.katari.sourcemigration.work.SourceMigrationDiscoveryWorker
import app.katari.sourcemigration.work.SourceMigrationExecutionWorker
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.StringKey
import kotlin.reflect.KClass

/** The app's workers under the names they were queued with before their packages moved to `app.katari`. */
@BindingContainer
object RetiredWorkerBindings {
    @Provides
    @IntoMap
    @StringKey("mihon.app.sync.SyncJob")
    fun retiredSyncJob(): KClass<out ListenableWorker> = SyncJob::class

    @Provides
    @IntoMap
    @StringKey("mihon.feature.migration.work.SourceMigrationDiscoveryWorker")
    fun retiredSourceMigrationDiscoveryWorker(): KClass<out ListenableWorker> = SourceMigrationDiscoveryWorker::class

    @Provides
    @IntoMap
    @StringKey("mihon.feature.migration.work.SourceMigrationExecutionWorker")
    fun retiredSourceMigrationExecutionWorker(): KClass<out ListenableWorker> = SourceMigrationExecutionWorker::class
}

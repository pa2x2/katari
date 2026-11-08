package app.katari.sourcemigration.work

import app.katari.core.metro.AppCoroutineScope
import app.katari.sourcemigration.discovery.SourceMigrationDiscoveryRunner
import app.katari.sourcemigration.execution.SourceMigrationExecutionRunner
import kotlinx.coroutines.CoroutineScope

/** Graph entry points of the Source Migration workers and notification actions, which Android constructs. */
interface SourceMigrationWorkGraph {
    val sourceMigrationDiscoveryRunner: SourceMigrationDiscoveryRunner
    val sourceMigrationExecutionRunner: SourceMigrationExecutionRunner
    val sourceMigrationWorkScheduler: SourceMigrationWorkScheduler

    @AppCoroutineScope
    val appScope: CoroutineScope
}

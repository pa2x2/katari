package mihon.feature.migration.work

import kotlinx.coroutines.CoroutineScope
import mihon.core.metro.AppCoroutineScope
import mihon.feature.migration.discovery.SourceMigrationDiscoveryRunner
import mihon.feature.migration.execution.SourceMigrationExecutionRunner

/** Graph entry points of the Source Migration workers and notification actions, which Android constructs. */
interface SourceMigrationWorkGraph {
    val sourceMigrationDiscoveryRunner: SourceMigrationDiscoveryRunner
    val sourceMigrationExecutionRunner: SourceMigrationExecutionRunner
    val sourceMigrationWorkScheduler: SourceMigrationWorkScheduler

    @AppCoroutineScope
    val appScope: CoroutineScope
}

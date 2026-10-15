package mihon.core.migration

class MigrationContext(
    val dryrun: Boolean,
    val previousVersion: Int,
    graph: MigrationGraph,
) : MigrationGraph by graph

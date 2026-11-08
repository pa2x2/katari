package app.katari.upgrade

class MigrationContext(
    val dryrun: Boolean,
    val previousVersion: Int,
    graph: MigrationGraph,
) : MigrationGraph by graph

package app.katari.sync.client.capture

import app.katari.sync.client.kind.SyncKinds

/** The statements that install every kind's capture triggers on a connection, and the ones that remove them. */
internal object CaptureTriggers {
    private val triggers = SyncKinds.all.flatMap { kind ->
        kind.capture.mapIndexed { index, trigger ->
            trigger.name(kind.name, index) to trigger.createStatement(kind.name, index)
        }
    }

    val remove: List<String> = triggers.map { (name, _) -> "DROP TRIGGER IF EXISTS temp.$name" }

    val install: List<String> = remove + triggers.map { (_, create) -> create }
}

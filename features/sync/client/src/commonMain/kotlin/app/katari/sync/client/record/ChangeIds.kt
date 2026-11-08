package app.katari.sync.client.record

import kotlin.uuid.Uuid

/** A new change ID ([app.katari.sync.protocol.record.RecordEnvelope.changeId]) for a record change to push. */
internal fun newChangeId(): String = Uuid.random().toString()

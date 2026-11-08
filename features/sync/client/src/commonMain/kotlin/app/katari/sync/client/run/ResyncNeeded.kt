package app.katari.sync.client.run

import app.katari.sync.client.connection.SyncStage
import app.katari.sync.protocol.server.ServerIdentity

/** The server's answer shows the device's position means nothing there: it resyncs in full at [stage]. */
internal class ResyncNeeded(
    val stage: SyncStage,
    val server: ServerIdentity?,
) : Exception("Full resync needed at $stage")

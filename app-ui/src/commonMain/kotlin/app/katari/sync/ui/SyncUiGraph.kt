package app.katari.sync.ui

import app.katari.sync.client.preference.SyncPreferences
import app.katari.sync.ui.status.SyncStatusState

/** Graph entry points of the screens and rows that show sync, beside their view models. */
interface SyncUiGraph {
    val syncStatusState: SyncStatusState
    val syncPreferences: SyncPreferences
}

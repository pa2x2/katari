package app.katari.ui.telemetry

import eu.kanade.tachiyomi.source.entry.EntryType

/** A screen about one entry, whose type crash reports carry along with the screen; see [ShownScreenReport]. */
interface EntryTypeScreen {
    /** Null until the screen knows it. Read in a snapshot, so a screen that learns it later says so. */
    val entryType: EntryType?
}

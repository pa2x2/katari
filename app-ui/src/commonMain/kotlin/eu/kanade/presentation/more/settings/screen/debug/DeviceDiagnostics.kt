package eu.kanade.presentation.more.settings.screen.debug

import app.katari.core.common.platform.PlatformFacility
import app.katari.core.platform.PlatformContext
import eu.kanade.presentation.util.Screen
import tachiyomi.domain.storage.DocumentUri

/**
 * What goes into a bug report about the device the app runs on: the app's logs, a summary of the build and device, the
 * device's facts and the app's background work. Where the platform offers part of it, the rows for the rest are hidden.
 */
@PlatformFacility
interface DeviceDiagnostics {
    /**
     * Whether dumped logs go into a document the user picks, through [dumpLogsToDocument], rather than to the
     * platform's share sheet, through [dumpLogs].
     */
    val savesLogsToPickedDocument: Boolean

    /**
     * Writes the app's logs and the [debugInfo], with [exception] when the crash screen reports one, to a file and
     * offers to share it. Only called while not [savesLogsToPickedDocument].
     */
    suspend fun dumpLogs(context: PlatformContext, exception: Throwable?)

    /** Writes what [dumpLogs] writes into [document]. Only called while [savesLogsToPickedDocument]. */
    suspend fun dumpLogsToDocument(document: DocumentUri, exception: Throwable?)

    /** The build and device summary that About copies for a bug report. */
    suspend fun debugInfo(context: PlatformContext): String

    /** False where the app's code isn't compiled ahead from a startup profile, which hides its status. */
    val reportsProfileCompilation: Boolean

    /** How far the app's code is compiled ahead of its startup, in words. Only called while [reportsProfileCompilation]. */
    suspend fun profileCompilationStatus(): String

    /** The debug info's device rows in order. */
    fun deviceFacts(): List<DeviceFact>

    /** False where the platform keeps no list of the app's background work, which hides it. */
    val listsBackgroundWork: Boolean

    /** The screen listing the app's scheduled, running and finished background work. Only called while [listsBackgroundWork]. */
    fun backgroundWorkScreen(): Screen
}

data class DeviceFact(val title: String, val value: String)

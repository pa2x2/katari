package eu.kanade.presentation.more.settings.screen.debug

import eu.kanade.presentation.util.Screen
import mihon.core.platform.PlatformContext

/**
 * What goes into a bug report about the device the app runs on: the app's logs, a summary of the build and device, the
 * device's facts and the app's background work. Where the platform offers none of it, the rows for it are hidden.
 */
interface DeviceDiagnostics {
    val isOffered: Boolean

    /** Writes the app's logs and the [debugInfo] to a file and offers to share it. Only called while [isOffered]. */
    suspend fun dumpLogs(context: PlatformContext)

    /** The build and device summary that About copies for a bug report. Only called while [isOffered]. */
    suspend fun debugInfo(context: PlatformContext): String

    /** How far the app's code is compiled ahead of its startup, in words. Only called while [isOffered]. */
    suspend fun profileCompilationStatus(): String

    /** The debug info's device rows in order. Only called while [isOffered]. */
    fun deviceFacts(): List<DeviceFact>

    /** The screen listing the app's scheduled, running and finished background work. Only called while [isOffered]. */
    fun backgroundWorkScreen(): Screen
}

data class DeviceFact(val title: String, val value: String)

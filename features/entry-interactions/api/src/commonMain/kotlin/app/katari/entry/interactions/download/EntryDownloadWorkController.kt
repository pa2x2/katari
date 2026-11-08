package app.katari.entry.interactions.download

import app.katari.core.common.platform.PlatformFacility

/** Owns the process-resilient execution lifetime for the shared download queue. */
@PlatformFacility
interface EntryDownloadWorkController {
    fun start()
    fun stop()

    /** Restores execution only when the last explicit user intent was to keep downloading. */
    fun resumeIfRequested() = Unit
}

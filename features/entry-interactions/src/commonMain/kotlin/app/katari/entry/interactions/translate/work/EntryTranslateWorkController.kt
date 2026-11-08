package app.katari.entry.interactions.translate.work

import app.katari.core.common.platform.PlatformFacility

/** Starts the platform's background work that drains the translation queue with [EntryTranslateRun]. */
@PlatformFacility
interface EntryTranslateWorkController {
    fun start()
}

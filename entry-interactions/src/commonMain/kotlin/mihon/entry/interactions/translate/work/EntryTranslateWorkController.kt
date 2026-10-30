package mihon.entry.interactions.translate.work

import mihon.core.common.platform.PlatformFacility

/** Starts the platform's background work that drains the translation queue with [EntryTranslateRun]. */
@PlatformFacility
interface EntryTranslateWorkController {
    fun start()
}

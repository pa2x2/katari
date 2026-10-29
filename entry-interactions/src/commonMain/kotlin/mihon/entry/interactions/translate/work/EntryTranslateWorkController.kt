package mihon.entry.interactions.translate.work

/** Starts the platform's background work that drains the translation queue with [EntryTranslateRun]. */
interface EntryTranslateWorkController {
    fun start()
}

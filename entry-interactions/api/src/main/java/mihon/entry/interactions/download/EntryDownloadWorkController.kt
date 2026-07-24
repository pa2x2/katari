package mihon.entry.interactions

/** Owns the process-resilient execution lifetime for the shared download queue. */
interface EntryDownloadWorkController {
    fun start()
    fun stop()
}

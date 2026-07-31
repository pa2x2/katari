package mihon.entry.interactions.download

/** Owns the process-resilient execution lifetime for the shared download queue. */
interface EntryDownloadWorkController {
    fun start()
    fun stop()
}

package app.katari.sync.client.downloadrequest

/**
 * Carries out the download requests other devices of the account send here, once their entry is here: the app
 * provides it, as downloads are the app's. Sync keeps a request until [perform] says it is [DownloadRequestResult.DONE]
 * or the request expires.
 */
fun interface DownloadRequestPerformer {
    suspend fun perform(request: ReceivedDownloadRequest): DownloadRequestResult
}

/** A download request for the entry [entryId] here, from the device named [fromName], or null when it isn't listed. */
data class ReceivedDownloadRequest(
    val entryId: Long,
    val chapters: List<RequestedChapter>,
    val fromName: String?,
)

enum class DownloadRequestResult {
    /** Carried out, or told as impossible: the request is dropped. */
    DONE,

    /** Something it needs may come later, such as an answer from the source: tried again after the next sync. */
    TRY_LATER,
}

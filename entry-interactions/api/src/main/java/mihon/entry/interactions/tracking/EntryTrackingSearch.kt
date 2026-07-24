package mihon.entry.interactions

sealed interface EntryTrackingSearchResult {
    data class Found(
        val candidates: List<EntryTrackingSearchCandidate>,
    ) : EntryTrackingSearchResult

    data class Unavailable(
        val reason: EntryTrackingOperationUnavailableReason,
    ) : EntryTrackingSearchResult

    data class Failed(
        val cause: Throwable,
    ) : EntryTrackingSearchResult
}

data class EntryTrackingSearchCandidate(
    val serviceId: EntryTrackingServiceId,
    val localId: Long?,
    val entryId: Long,
    val remoteId: Long,
    val libraryId: Long?,
    val title: String,
    val progress: Double,
    val total: Long,
    val score: Double,
    val status: Long,
    val startDate: Long,
    val finishDate: Long,
    val private: Boolean,
    val remoteUrl: String,
    val authors: List<String>,
    val artists: List<String>,
    val coverUrl: String,
    val summary: String,
    val publishingStatus: String,
    val publishingType: String,
    val publicationStartDate: String,
)

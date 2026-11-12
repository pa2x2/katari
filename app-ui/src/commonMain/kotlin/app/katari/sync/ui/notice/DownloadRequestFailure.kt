package app.katari.sync.ui.notice

/**
 * A download request another device sent that this device couldn't carry out, which a notification tells, one per
 * entry. Tapping it opens the entry.
 */
data class DownloadRequestFailure(
    val entryId: Long,
    val profileId: Long,
    val title: String,
    /** The device that asked, null when the account doesn't list it. */
    val from: String?,
    /** The chapters it couldn't download, by the names the other device showed. */
    val chapters: List<String>,
    val sourceName: String,
    val reason: Reason,
) {
    enum class Reason {
        /** The entry's source isn't installed here. */
        SOURCE_MISSING,

        /** The source no longer lists the chapters, even after a refresh. */
        CHAPTERS_GONE,
    }
}

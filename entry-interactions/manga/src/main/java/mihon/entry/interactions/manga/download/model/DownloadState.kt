package mihon.entry.interactions.manga.download.model

internal enum class DownloadState(val value: Int) {
    NOT_DOWNLOADED(0),
    QUEUE(1),
    DOWNLOADING(2),
    DOWNLOADED(3),
    ERROR(4),
}

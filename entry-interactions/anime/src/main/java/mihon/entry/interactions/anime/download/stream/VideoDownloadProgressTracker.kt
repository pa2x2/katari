package mihon.entry.interactions.anime.download.stream

import mihon.entry.interactions.anime.download.model.AnimeDownload

internal class VideoDownloadProgressTracker(
    private val download: AnimeDownload,
) {
    private var totalBytesExpected = 0L
    private var downloadedBytes = 0L
    private var transferCompleted = false
    private var isHls = false
    private var sizeEstimated = false
    private var totalAssetsExpected = 0
    private var downloadedAssets = 0

    fun markAsHls() {
        isHls = true
    }

    /** A video in many files whose size is estimated up front, so the files' own lengths don't add to it. */
    fun expectBytes(total: Long) {
        sizeEstimated = true
        totalBytesExpected = total
        update()
    }

    fun onAssetsFound(count: Int) {
        totalAssetsExpected += count
        update()
    }

    fun onAssetDownloaded() {
        downloadedAssets++
        update()
    }

    fun onResponseStarted(contentLength: Long?) {
        if (isHls || sizeEstimated) return
        if (contentLength != null) {
            totalBytesExpected += contentLength
            update()
        }
    }

    fun onBytesCopied(read: Long) {
        if (isHls) return
        downloadedBytes += read
        update()
    }

    fun onResponseFinished(contentLengthOrActual: Long) {
        if (isHls || sizeEstimated) return
        if (totalBytesExpected < downloadedBytes) {
            totalBytesExpected = downloadedBytes
        }
        if (totalBytesExpected == 0L) {
            totalBytesExpected = contentLengthOrActual
        }
        update()
    }

    fun onVideoTransferComplete() {
        transferCompleted = true
        download.progress = maxOf(download.progress, 90)
    }

    fun onNonVideoPhaseProgress(progress: Int) {
        transferCompleted = true
        download.progress = maxOf(download.progress, progress)
    }

    private fun update() {
        if (transferCompleted) return
        val fraction = if (isHls) {
            if (totalAssetsExpected <= 0) 0.0 else downloadedAssets.toDouble() / totalAssetsExpected.toDouble()
        } else {
            if (totalBytesExpected <= 0L) return
            downloadedBytes.toDouble() / totalBytesExpected.toDouble()
        }
        val next = (fraction.coerceIn(0.0, 1.0) * 90.0).toInt().coerceIn(0, 90)
        download.progress = maxOf(download.progress, next)
    }
}

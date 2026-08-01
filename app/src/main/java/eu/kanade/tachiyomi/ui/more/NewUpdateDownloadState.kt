package eu.kanade.tachiyomi.ui.more

import androidx.work.WorkInfo
import eu.kanade.tachiyomi.data.updater.AppUpdateDownloadJob

internal data class UpdateDownloadWork(
    val state: WorkInfo.State,
    val url: String?,
    val progress: Int?,
)

internal data class UpdateDownloadStatus(
    val stage: NewUpdateScreenModel.Stage,
    val progress: Int? = null,
)

internal fun resolveUpdateDownloadStatus(
    work: UpdateDownloadWork?,
    downloadLink: String,
    downloadedApkMatches: Boolean,
): UpdateDownloadStatus {
    if (work == null || work.url != downloadLink) {
        val stage = if (downloadedApkMatches) {
            NewUpdateScreenModel.Stage.Downloaded
        } else {
            NewUpdateScreenModel.Stage.Available
        }
        return UpdateDownloadStatus(stage)
    }

    return when (work.state) {
        WorkInfo.State.ENQUEUED,
        WorkInfo.State.RUNNING,
        WorkInfo.State.BLOCKED,
        -> UpdateDownloadStatus(NewUpdateScreenModel.Stage.Downloading, work.progress)
        WorkInfo.State.SUCCEEDED -> UpdateDownloadStatus(
            if (downloadedApkMatches) NewUpdateScreenModel.Stage.Downloaded else NewUpdateScreenModel.Stage.Available,
        )
        WorkInfo.State.FAILED -> UpdateDownloadStatus(NewUpdateScreenModel.Stage.Failed)
        WorkInfo.State.CANCELLED -> UpdateDownloadStatus(NewUpdateScreenModel.Stage.Available)
    }
}

internal fun WorkInfo.toUpdateDownloadWork(): UpdateDownloadWork {
    val data = if (state.isFinished) outputData else progress
    return UpdateDownloadWork(
        state = state,
        url = data.getString(AppUpdateDownloadJob.EXTRA_DOWNLOAD_URL)
            ?: AppUpdateDownloadJob.downloadUrlFromTags(tags),
        progress = data.getInt(AppUpdateDownloadJob.PROGRESS, -1).takeIf { it >= 0 },
    )
}

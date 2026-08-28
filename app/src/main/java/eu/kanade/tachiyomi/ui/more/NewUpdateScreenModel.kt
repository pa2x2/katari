package eu.kanade.tachiyomi.ui.more

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.data.updater.AppUpdateDownloadJob
import eu.kanade.tachiyomi.extension.util.ExtensionInstaller
import eu.kanade.tachiyomi.util.storage.getUriCompat
import eu.kanade.tachiyomi.util.system.workManager
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class NewUpdateScreenModel(
    changelogInfo: String,
    private val downloadLink: String,
    private val context: Context = Injekt.get(),
) : StateScreenModel<NewUpdateScreenModel.State>(State(changelogInfo = sanitizeInAppReleaseNotes(changelogInfo))) {

    init {
        context.workManager.getWorkInfosForUniqueWorkFlow(AppUpdateDownloadJob.TAG)
            .map { workInfos ->
                resolveUpdateDownloadStatus(
                    work = workInfos.firstOrNull()?.toUpdateDownloadWork(),
                    downloadLink = downloadLink,
                    downloadedApkMatches = AppUpdateDownloadJob.isDownloaded(context, downloadLink),
                )
            }
            .distinctUntilChanged()
            .onEach { status ->
                mutableState.update {
                    it.copy(
                        downloadProgress = status.progress,
                        stage = status.stage,
                    )
                }
            }
            .launchIn(screenModelScope)
    }

    fun startDownload() {
        mutableState.update { it.copy(downloadProgress = null, stage = Stage.Downloading) }
        AppUpdateDownloadJob.start(context, downloadLink)
    }

    fun installUpdate() {
        if (!AppUpdateDownloadJob.isDownloaded(context, downloadLink)) {
            mutableState.update { it.copy(downloadProgress = null, stage = Stage.Available) }
            return
        }

        val apkFile = AppUpdateDownloadJob.updateApk(context)
        val uri = apkFile.getUriCompat(context)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            clipData = ClipData.newRawUri(null, uri)
            setDataAndType(uri, ExtensionInstaller.APK_MIME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }

    @Immutable
    data class State(
        val changelogInfo: String,
        val downloadProgress: Int? = null,
        val stage: Stage = Stage.Available,
    )

    enum class Stage {
        Available,
        Downloading,
        Downloaded,
        Failed,
    }
}

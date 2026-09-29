package eu.kanade.tachiyomi.ui.more

import androidx.work.WorkInfo
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class NewUpdateDownloadStateTest {

    private val downloadLink = "https://example.com/katari.apk"

    @Test
    fun `work for another URL cannot expose a stale APK`() {
        resolveUpdateDownloadStatus(
            work = work(WorkInfo.State.SUCCEEDED, url = "https://example.com/old.apk", progress = 100),
            downloadLink = downloadLink,
            downloadedApkMatches = false,
        ) shouldBe UpdateDownloadStatus(NewUpdateScreenModel.Stage.Available)
    }

    @Test
    fun `downloaded APK remains recoverable after WorkManager prunes its work`() {
        resolveUpdateDownloadStatus(
            work = null,
            downloadLink = downloadLink,
            downloadedApkMatches = true,
        ) shouldBe UpdateDownloadStatus(NewUpdateScreenModel.Stage.Downloaded)
    }

    private fun work(
        state: WorkInfo.State,
        url: String = downloadLink,
        progress: Int? = null,
    ) = UpdateDownloadWork(state, url, progress)
}

package eu.kanade.tachiyomi.data.updater

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class AppUpdateDownloadJobTest {

    @Test
    fun `unknown content length does not publish invalid progress`() {
        calculateProgress(bytesRead = 512, contentLength = -1).shouldBeNull()
        calculateProgress(bytesRead = 0, contentLength = 0).shouldBeNull()
    }

    @Test
    fun `progress remains below completion until the download succeeds`() {
        calculateProgress(bytesRead = 50, contentLength = 100) shouldBe 50
        calculateProgress(bytesRead = 100, contentLength = 100) shouldBe 99
    }

    @Test
    fun `download URL is recovered from the work tag before progress starts`() {
        AppUpdateDownloadJob.downloadUrlFromTags(
            setOf(AppUpdateDownloadJob.TAG, "${AppUpdateDownloadJob.TAG}:https://example.com/katari.apk"),
        ) shouldBe "https://example.com/katari.apk"
    }
}

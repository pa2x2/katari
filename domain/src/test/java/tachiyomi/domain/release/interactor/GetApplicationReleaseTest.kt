package tachiyomi.domain.release.interactor

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.release.model.Release
import tachiyomi.domain.release.service.ReleaseService

class GetApplicationReleaseTest {

    private val releaseService = mockk<ReleaseService>()
    private val getApplicationRelease = GetApplicationRelease(releaseService)

    @Test
    fun `newer preview and release tags are updates while the current release is not`() = runTest {
        suspend fun check(tag: String, isPreview: Boolean, commitCount: Int, versionName: String): Any {
            coEvery { releaseService.latest(any()) } returns Release(
                tag,
                "info",
                "http://example.com/release_link",
                "http://example.com/release_link.apk",
            )
            return getApplicationRelease.await(
                GetApplicationRelease.Arguments(
                    isFoss = false,
                    isPreview = isPreview,
                    commitCount = commitCount,
                    versionName = versionName,
                    repository = "test",
                ),
            ).let { (it as? GetApplicationRelease.Result.NewUpdate)?.release?.version ?: it }
        }

        check("r2000", isPreview = true, commitCount = 1000, versionName = "") shouldBe "r2000"
        check("v2.0.0", isPreview = false, commitCount = 0, versionName = "v1.0.0") shouldBe "v2.0.0"
        check("v1.0.0", isPreview = false, commitCount = 0, versionName = "v2.0.0") shouldBe
            GetApplicationRelease.Result.NoNewUpdate
    }
}

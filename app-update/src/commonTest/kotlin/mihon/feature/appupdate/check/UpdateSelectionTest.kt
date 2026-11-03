package mihon.feature.appupdate.check

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class UpdateSelectionTest {

    @Test
    fun `stable skips drafts and both kinds of pre-release, and notes cover every release since the installed one`() {
        val releases = listOf(
            release("v1.15.0", draft = true),
            release("v1.14.0-beta1"),
            release("v1.14.0", prerelease = true),
            release("v1.13.1"),
            release("v1.13.0", body = ""),
            release("v1.12.1"),
            release("v1.12.0"),
        )

        val stable = select(releases, "v", AppUpdateChannel.STABLE, "1.12.0")!!
        stable.version shouldBe "1.13.1"
        stable.notes.map { it.version } shouldBe listOf("1.13.1", "1.12.1")
        stable.asset.sha256 shouldBe "ab".repeat(32)

        val prerelease = select(releases, "v", AppUpdateChannel.PRERELEASE, "1.12.0")!!
        prerelease.version shouldBe "1.14.0"
        prerelease.prerelease shouldBe true

        select(releases, "v", AppUpdateChannel.STABLE, "1.13.1").shouldBeNull()
    }

    @Test
    fun `the Android and desktop apps release from one repository and are each offered only their own releases`() {
        val releases = listOf(
            release("desktop-v3.0.0"),
            release("v2.1.0"),
            release("desktop-v0.2.0-alpha.1"),
            release("v2.0.0"),
            release("desktop-v0.1.0-alpha.1"),
        )

        val android = select(releases, "v", AppUpdateChannel.PRERELEASE, "2.0.0")!!
        android.version shouldBe "2.1.0"
        android.notes.map { it.version } shouldBe listOf("2.1.0")

        val desktop = select(releases, "desktop-v", AppUpdateChannel.PRERELEASE, "0.1.0-alpha.1")!!
        desktop.version shouldBe "3.0.0"
        desktop.notes.map { it.version } shouldBe listOf("3.0.0", "0.2.0-alpha.1")
    }

    private fun select(releases: List<GithubRelease>, tagPrefix: String, channel: AppUpdateChannel, installed: String) =
        selectUpdate(releases, tagPrefix, channel, installed, selectAsset = { it.firstOrNull() })

    private fun release(
        tag: String,
        draft: Boolean = false,
        prerelease: Boolean = false,
        body: String = "Notes of $tag",
    ) = GithubRelease(
        tagName = tag,
        draft = draft,
        prerelease = prerelease,
        body = body,
        pageUrl = "https://github.com/pa2x2/katari/releases/tag/$tag",
        assets = listOf(
            GithubAsset(
                name = "katari-$tag",
                downloadUrl = "https://example.org/katari-$tag",
                size = 1,
                digest = "sha256:" + "AB".repeat(32),
            ),
        ),
    )
}

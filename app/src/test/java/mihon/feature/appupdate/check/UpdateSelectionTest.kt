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

        val stable = selectUpdate(releases, AppUpdateChannel.STABLE, "1.12.0", isFoss = false, primaryAbi = ABI)!!
        stable.version shouldBe "1.13.1"
        stable.notes.map { it.version } shouldBe listOf("1.13.1", "1.12.1")
        stable.apk.sha256 shouldBe "ab".repeat(32)

        val prerelease = selectUpdate(
            releases,
            AppUpdateChannel.PRERELEASE,
            "1.12.0",
            isFoss = false,
            primaryAbi = ABI,
        )!!
        prerelease.version shouldBe "1.14.0"
        prerelease.prerelease shouldBe true

        selectUpdate(releases, AppUpdateChannel.STABLE, "1.13.1", isFoss = false, primaryAbi = ABI).shouldBeNull()
    }

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
                name = "katari-$ABI-$tag.apk",
                downloadUrl = "https://example.org/katari-$ABI-$tag.apk",
                size = 1,
                digest = "sha256:" + "AB".repeat(32),
            ),
        ),
    )

    private companion object {
        const val ABI = "arm64-v8a"
    }
}

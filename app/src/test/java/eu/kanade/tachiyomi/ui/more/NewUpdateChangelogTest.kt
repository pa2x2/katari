package eu.kanade.tachiyomi.ui.more

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class NewUpdateChangelogTest {

    @Test
    fun `checksum section is hidden from release notes`() {
        sanitizeInAppReleaseNotes(
            """
            ## Changes
            - Fixed updater

            ---

            ## Checksums
            `katari.apk: abc123`
            """.trimIndent(),
        ).trimEnd() shouldBe """
            ## Changes
            - Fixed updater
        """.trimIndent()
    }

    @Test
    fun `earlier release note divider is preserved`() {
        sanitizeInAppReleaseNotes(
            """
            ## Changes
            - First group

            ---

            ## More changes
            - Second group

            ---

            ## Checksums
            `katari.apk: abc123`
            """.trimIndent(),
        ).trimEnd() shouldBe """
            ## Changes
            - First group

            ---

            ## More changes
            - Second group
        """.trimIndent()
    }

    @Test
    fun `download selection tip is hidden from in-app release notes`() {
        sanitizeInAppReleaseNotes(
            """
            ### Added

            - First change

            > [!TIP]
            >
            > If you are unsure which version to download, use `katari-v1.5.1.apk`.
            """.trimIndent().replace("\n", "\r\n"),
        ).trimEnd() shouldBe """
            ### Added

            - First change
        """.trimIndent().replace("\n", "\r\n")
    }

    @Test
    fun `other release note tips are preserved`() {
        val releaseNotes = """
            ### Added

            > [!TIP]
            >
            > Restart the app after updating extension sources.
        """.trimIndent()

        sanitizeInAppReleaseNotes(releaseNotes) shouldBe releaseNotes
    }
}

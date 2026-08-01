package eu.kanade.tachiyomi.ui.more

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class NewUpdateChangelogTest {

    @Test
    fun `checksum section is hidden from release notes`() {
        stripChecksumSection(
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
        stripChecksumSection(
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
}

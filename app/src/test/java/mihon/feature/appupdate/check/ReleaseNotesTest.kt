package mihon.feature.appupdate.check

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ReleaseNotesTest {

    @Test
    fun `the download tip release yml appends is dropped from the notes`() {
        // As GitHub returns the body release.yml writes: CRLF line endings and a trailing alert block.
        val body = "### ⚡️ Performance\r\n\r\n" +
            "- LibreTranslate requests no longer decrypt the saved API key every time.\r\n\r\n" +
            "> [!TIP]\r\n>\r\n> If you are unsure which version to download, use `katari-v1.12.0.apk`.\r\n"

        cleanReleaseNotes(body) shouldBe "### ⚡️ Performance\n\n" +
            "- LibreTranslate requests no longer decrypt the saved API key every time."
    }
}

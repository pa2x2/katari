package mihon.gradle.licences

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LibmpvNoticesTest {

    @Test
    fun `a header-shaped line inside a licence text stays in that text`() {
        val thirdParty = """
            {
              "source": "https://example.org/libmpv-1",
              "components": [
                {"name": "libc++", "version": "30", "license": "Apache-2.0 WITH LLVM-exception",
                 "url": "https://llvm.org", "binaries": "libc++_shared.so", "licenseTexts": ["libc++: LICENSE.TXT"]},
                {"name": "mpv", "version": "0.41.0", "license": "LGPL-2.1-or-later",
                 "url": "https://mpv.io", "binaries": "libmpv.so", "licenseTexts": ["mpv: LICENSE.LGPL"]}
              ]
            }
        """.trimIndent()
        val licences = """
            libmpv-1

            Source: https://example.org/libmpv-1

            ---- libc++: LICENSE.TXT ----

            Apache License
            ---- LLVM Exceptions to the Apache 2.0 License ----
            As an exception
            ---- mpv: LICENSE.LGPL ----
            GNU LESSER GENERAL PUBLIC LICENSE
        """.trimIndent()

        val notices = LibmpvNotices.parse(thirdParty, licences, "1")

        assertEquals("libmpv-1\n\nSource: https://example.org/libmpv-1", notices.notice)
        assertEquals(
            listOf(
                "Apache License\n---- LLVM Exceptions to the Apache 2.0 License ----\nAs an exception",
                "GNU LESSER GENERAL PUBLIC LICENSE",
            ),
            notices.components.map { it.licenceTexts.single().text },
        )
    }
}

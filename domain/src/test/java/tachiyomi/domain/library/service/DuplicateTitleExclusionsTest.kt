package tachiyomi.domain.library.service

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class DuplicateTitleExclusionsTest {

    @Test
    fun `sanitize preserves order and removes invalid duplicates`() {
        DuplicateTitleExclusions.sanitizePatterns(
            listOf(" [*] ", "(*)", "[*]", "**", "(*)"),
        ) shouldBe listOf("[*]", "(*)")
    }

    @Test
    fun `trailing wildcard matches to the end`() {
        val regex = DuplicateTitleExclusions.compilePatterns(listOf("edition *")).single().regex

        "One Punch Man Edition English".replace(regex, " ") shouldBe "One Punch Man  "
    }

    @Test
    fun `bracket wildcard stays local to each segment`() {
        val regex = DuplicateTitleExclusions.compilePatterns(listOf("[*]")).single().regex

        "One Punch Man [English] [Scanlator]".replace(regex, " ") shouldBe "One Punch Man    "
    }
}

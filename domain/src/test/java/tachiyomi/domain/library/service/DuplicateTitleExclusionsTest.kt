package tachiyomi.domain.library.service

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class DuplicateTitleExclusionsTest {

    @Test
    fun `trailing wildcard matches to the end while bracket wildcard stays local to each segment`() {
        val trailing = DuplicateTitleExclusions.compilePatterns(listOf("edition *")).single().regex
        val bracket = DuplicateTitleExclusions.compilePatterns(listOf("[*]")).single().regex

        "One Punch Man Edition English".replace(trailing, " ") shouldBe "One Punch Man  "
        "One Punch Man [English] [Scanlator]".replace(bracket, " ") shouldBe "One Punch Man    "
    }
}

package eu.kanade.tachiyomi.util.lang

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class StringExtensionsTest {

    @Test
    fun `toStoredDisplayName trims custom value`() {
        "  Custom Title  ".toStoredDisplayName(sourceTitle = "Source Title") shouldBe "Custom Title"
    }

    @Test
    fun `toStoredDisplayName returns null for blank input`() {
        "   ".toStoredDisplayName(sourceTitle = "Source Title").shouldBeNull()
    }

    @Test
    fun `toStoredDisplayName returns null when equal to source title after trim`() {
        "  Source Title  ".toStoredDisplayName(sourceTitle = "Source Title").shouldBeNull()
    }
}

package mihon.entry.interactions.book.reader.language

import io.kotest.matchers.shouldBe
import mihon.language.api.tag.LanguageTag
import org.junit.jupiter.api.Test

class BookSelectionLanguageSessionTest {
    @Test
    fun `resolved language becomes the in-memory prior for later selections`() {
        val session = BookSelectionLanguageSession(listOf("en_US", "und", "en-US"))

        session.context("first paragraph").declaredLanguages shouldBe listOf(LanguageTag.require("en-US"))
        session.context("first paragraph").sessionLanguage shouldBe null

        session.record(LanguageTag.require("fr"))

        val next = session.context("second paragraph")
        next.sessionLanguage shouldBe LanguageTag.require("fr")
        next.surroundingText shouldBe "second paragraph"
    }
}

package mihon.entry.interactions.book.reader.language

import io.kotest.matchers.shouldBe
import mihon.language.api.tag.LanguageTag
import org.junit.jupiter.api.Test

class BookSelectionLanguageSessionTest {
    @Test
    fun `resolved language becomes the in-memory prior for later selections`() {
        val session = BookSelectionLanguageSession(listOf("en_US", "und", "en-US"))

        session.context("first paragraph", emptyList()).declaredLanguages shouldBe listOf(LanguageTag.require("en-US"))
        session.context("first paragraph", emptyList()).sessionLanguage shouldBe null

        session.record(LanguageTag.require("fr"))

        val next = session.context("second paragraph", emptyList())
        next.sessionLanguage shouldBe LanguageTag.require("fr")
        next.surroundingText shouldBe "second paragraph"
    }

    @Test
    fun `languages declared by the selected text come before the publication's`() {
        val session = BookSelectionLanguageSession(listOf("en"))

        session.context("paragraph", selectionLanguageTags = listOf("fr", "en")).declaredLanguages shouldBe
            listOf(LanguageTag.require("fr"), LanguageTag.require("en"))
    }
}

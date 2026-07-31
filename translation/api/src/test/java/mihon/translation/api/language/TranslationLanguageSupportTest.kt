package mihon.translation.api.language

import io.kotest.assertions.throwables.shouldThrow
import org.junit.jupiter.api.Test

class TranslationLanguageSupportTest {
    @Test
    fun `language support rejects empty catalogs and identity pairs`() {
        shouldThrow<IllegalArgumentException> {
            TranslationLanguageSupport.ExactPairs(emptySet())
        }
        shouldThrow<IllegalArgumentException> {
            TranslationLanguageSupport.ByRole(emptySet(), setOf(ENGLISH))
        }
        shouldThrow<IllegalArgumentException> {
            TranslationLanguageSupport.ByRole(setOf(ENGLISH), emptySet())
        }
        shouldThrow<IllegalArgumentException> {
            TranslationLanguagePair(ENGLISH, ENGLISH)
        }
    }

    private companion object {
        val ENGLISH = TranslationLanguageTag.require("en")
    }
}

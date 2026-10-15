package mihon.entry.interactions.translation

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import mihon.language.api.tag.LanguageTag
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry

class EntryTranslationLanguagesFeatureTest {
    private val target = entry(id = 8L)

    @Test
    fun `backup restore replaces the entry's languages and drops tags that no longer parse`() = runTest {
        val feature = featureFor(InMemoryEntryTranslationLanguagesRepository())
        feature.setContentLanguage(target, JAPANESE)

        feature.restore(
            target,
            EntryTranslationLanguagesSnapshot(contentLanguage = "und", targetLanguage = "es", updatedAt = 3L),
        )

        feature.observe(target).first() shouldBe EntryTranslationLanguageChoices(targetLanguage = SPANISH)
    }

    private fun featureFor(
        repository: InMemoryEntryTranslationLanguagesRepository,
    ): DefaultEntryTranslationLanguagesFeature {
        return DefaultEntryTranslationLanguagesFeature(
            applicableTypes = setOf(EntryType.BOOK),
            migrationTypes = emptySet(),
            repository = repository,
            clock = { 1L },
        )
    }

    private fun entry(id: Long): Entry = Entry.create().copy(id = id, type = EntryType.BOOK)

    private companion object {
        val JAPANESE = LanguageTag.require("ja")
        val SPANISH = LanguageTag.require("es")
    }
}

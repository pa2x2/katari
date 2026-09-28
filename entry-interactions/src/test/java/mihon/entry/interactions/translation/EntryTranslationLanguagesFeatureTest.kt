package mihon.entry.interactions.translation

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.runtime.EntryInteractionPlugin
import mihon.entry.interactions.runtime.EntryInteractionProviderBinding
import mihon.entry.interactions.runtime.createEntryInteractionComposition
import mihon.entry.interactions.state.EntryMigrationCapability
import mihon.entry.interactions.state.EntryMigrationProvider
import mihon.entry.interactions.state.EntryTranslationLanguagesCapability
import mihon.entry.interactions.state.EntryTranslationLanguagesProvider
import mihon.feature.graph.ContributionOwner
import mihon.language.api.tag.LanguageTag
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry

class EntryTranslationLanguagesFeatureTest {
    private val source = entry(id = 7L)
    private val target = entry(id = 8L)

    @Test
    fun `migration carries the target language and leaves the content language to the new source`() = runTest {
        val feature = featureFor(
            InMemoryEntryTranslationLanguagesRepository(),
            TranslationLanguages,
            EntryMigrationCapability.bind(MigrationProvider),
        )
        feature.setContentLanguage(source, JAPANESE)
        feature.setTargetLanguage(source, SPANISH)

        val prepared = feature.prepareMigration(source, target)
            as EntryTranslationLanguagesMigrationPreparation.Prepared
        feature.applyMigration(prepared.payload)

        feature.observe(target).first() shouldBe EntryTranslationLanguageChoices(targetLanguage = SPANISH)
    }

    @Test
    fun `backup restore replaces the entry's languages and drops tags that no longer parse`() = runTest {
        val feature = featureFor(InMemoryEntryTranslationLanguagesRepository(), TranslationLanguages)
        feature.setContentLanguage(target, JAPANESE)

        feature.restore(
            target,
            EntryTranslationLanguagesSnapshot(contentLanguage = "und", targetLanguage = "es", updatedAt = 3L),
        )

        feature.observe(target).first() shouldBe EntryTranslationLanguageChoices(targetLanguage = SPANISH)
    }

    private fun featureFor(
        repository: InMemoryEntryTranslationLanguagesRepository,
        vararg bindings: EntryInteractionProviderBinding<*>,
    ): DefaultEntryTranslationLanguagesFeature {
        val composition = createEntryInteractionComposition(
            plugins = listOf(
                object : EntryInteractionPlugin {
                    override val type = EntryType.BOOK
                    override val owner = ContributionOwner("test.translation-languages-type")
                    override val providerBindings = bindings.toList()
                },
            ),
            featureContributors = listOf(EntryTranslationLanguagesFeatureContributor),
        )
        return DefaultEntryTranslationLanguagesFeature(
            evaluation = composition.featureGraphEvaluation,
            repository = repository,
            clock = { 1L },
        )
    }

    private fun entry(id: Long): Entry = Entry.create().copy(id = id, type = EntryType.BOOK)

    private object BookTranslationLanguages : EntryTranslationLanguagesProvider {
        override val type = EntryType.BOOK
    }

    private object MigrationProvider : EntryMigrationProvider {
        override val type = EntryType.BOOK
    }

    private companion object {
        val TranslationLanguages = EntryTranslationLanguagesCapability.bind(BookTranslationLanguages)
        val JAPANESE = LanguageTag.require("ja")
        val SPANISH = LanguageTag.require("es")
    }
}

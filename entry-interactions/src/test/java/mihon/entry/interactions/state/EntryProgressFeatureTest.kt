package mihon.entry.interactions.state

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import mihon.entry.interactions.media.session.EntryMediaSessionEvent
import mihon.entry.interactions.runtime.EntryInteractionComposition
import mihon.entry.interactions.runtime.EntryInteractionPlugin
import mihon.entry.interactions.runtime.EntryInteractionProviderBinding
import mihon.entry.interactions.runtime.createEntryInteractionComposition
import mihon.feature.graph.ContributionOwner
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryProgressLocator
import tachiyomi.domain.entry.model.EntryProgressState
import tachiyomi.domain.entry.repository.EntryProgressRepository

class EntryProgressFeatureTest {
    private val source = Entry.create().copy(id = 7L, type = EntryType.BOOK)

    @Test
    fun `media progress preserves existing locator extensions while incoming keys win`() = runTest {
        val child = EntryChapter.create().copy(id = 9L, entryId = source.id, url = "/chapter-1")
        val current = EntryProgressState(
            entryId = source.id,
            chapterId = child.id,
            resourceKey = child.url,
            locator = EntryProgressLocator(
                kind = "book",
                extensions = JsonObject(
                    mapOf(
                        "future.extension" to JsonPrimitive("preserved"),
                        "book.location" to JsonPrimitive("old"),
                    ),
                ),
            ),
        )
        val incoming = current.copy(
            locator = current.locator.copy(
                extensions = JsonObject(mapOf("book.location" to JsonPrimitive("new"))),
            ),
        )
        val recorded = slot<EntryProgressState>()
        val repository = mockk<EntryProgressRepository> {
            coEvery { get(source.id, "", child.url) } returns current
            coEvery { mergeAndSyncChild(capture(recorded)) } answers { recorded.captured }
        }
        val feature = featureFor(compositionFor(), repository)

        feature.recordMediaProgress(
            EntryMediaSessionEvent.Progressed(
                visibleEntry = source,
                child = child,
                progress = incoming,
                fraction = null,
                preserveLocatorExtensions = true,
            ),
        )

        recorded.captured.locator.extensions shouldBe JsonObject(
            mapOf(
                "future.extension" to JsonPrimitive("preserved"),
                "book.location" to JsonPrimitive("new"),
            ),
        )
    }

    private fun compositionFor(
        vararg bindings: EntryInteractionProviderBinding<*>,
    ): EntryInteractionComposition {
        return createEntryInteractionComposition(
            plugins = listOf(
                object : EntryInteractionPlugin {
                    override val type = EntryType.BOOK
                    override val owner = ContributionOwner("test.partial-progress-type")
                    override val providerBindings = bindings.toList()
                },
            ),
            featureContributors = listOf(EntryProgressFeatureContributor),
        )
    }

    private fun featureFor(
        composition: EntryInteractionComposition,
        repository: EntryProgressRepository = mockk(relaxed = true),
    ): EntryProgressFeature {
        return DefaultEntryProgressFeature(
            evaluation = composition.featureGraphEvaluation,
            interaction = composition.interactions.progress,
            repository = repository,
            getEntryWithChapters = mockk(relaxed = true),
            globalLibraryPreferences = mockk(relaxed = true),
        )
    }
}

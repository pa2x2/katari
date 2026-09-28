package mihon.entry.interactions.library

import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.navigation.EntryContinueCapability
import mihon.entry.interactions.navigation.EntryContinueFeature
import mihon.entry.interactions.navigation.EntryContinueFeatureContributor
import mihon.entry.interactions.navigation.EntryContinueProcessor
import mihon.entry.interactions.navigation.EntryContinueTargetResult
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
import tachiyomi.domain.entry.service.EntryLibraryContinueTarget
import tachiyomi.domain.entry.service.EntryLibraryProgressMember
import tachiyomi.domain.entry.service.EntryLibraryProgressResolution

class EntryLibraryProgressFeatureTest {
    private val entry = Entry.create().copy(id = 7L, type = EntryType.BOOK)

    @Test
    fun `batch calculation uses prepared evidence and owner resolved Continue targets`() = runTest {
        val chapter = chapter(id = 11L)
        val next = chapter(id = 12L)
        val progressState = EntryProgressState(
            entryId = entry.id,
            chapterId = chapter.id,
            resourceKey = "/chapter",
            locator = EntryProgressLocator(kind = "book", progression = 0.5),
            locatorUpdatedAt = 30L,
        )
        val composition = createEntryInteractionComposition(
            plugins = listOf(
                plugin(
                    EntryType.BOOK,
                    EntryLibraryProgressCapability.bind(PreparedEvidenceProvider()),
                    EntryContinueCapability.bind(ContinueProvider(next)),
                ),
            ),
            featureContributors = listOf(
                EntryContinueFeatureContributor,
                EntryLibraryProgressFeatureContributor,
            ),
        )
        val progressRepository = mockk<EntryProgressRepository> {
            coEvery { getByEntryIds(setOf(entry.id)) } returns listOf(progressState)
        }
        val continueFeature = mockk<EntryContinueFeature> {
            coEvery { nextTargets(listOf(entry)) } returns mapOf(
                entry.id to EntryContinueTargetResult.Available(next),
            )
        }
        val feature = DefaultEntryLibraryProgressFeature(
            evaluation = composition.featureGraphEvaluation,
            interaction = composition.interactions.libraryProgress,
            continueFeature = continueFeature,
            entryProgressRepository = progressRepository,
        )

        val result = feature.calculateBatch(
            listOf(EntryLibraryProgressMember(entry, listOf(chapter, next), lastRead = 20L)),
        ).getValue(entry.id).shouldBeInstanceOf<EntryLibraryProgressResolution.Available>().summary

        result.inProgressItemId shouldBe chapter.id
        result.continueTarget shouldBe EntryLibraryContinueTarget.Available(next.id)
    }

    private fun plugin(
        type: EntryType,
        vararg bindings: EntryInteractionProviderBinding<*>,
    ): EntryInteractionPlugin {
        return object : EntryInteractionPlugin {
            override val type = type
            override val owner = ContributionOwner("test.type.${type.name.lowercase()}")
            override val providerBindings = bindings.toList()
        }
    }

    private class PreparedEvidenceProvider : EntryLibraryProgressProvider {
        override val type = EntryType.BOOK

        override suspend fun evidence(
            entry: Entry,
            chapters: List<EntryChapter>,
        ): EntryLibraryProgressEvidence = error("Batch calculation must not load evidence per Entry")

        override suspend fun evidence(
            entry: Entry,
            chapters: List<EntryChapter>,
            progressStates: List<EntryProgressState>,
        ): EntryLibraryProgressEvidence {
            val state = progressStates.single()
            return EntryLibraryProgressEvidence(
                hasMediaProgress = true,
                inProgressItemId = state.chapterId,
                inProgressFraction = state.locator.progression?.toFloat(),
                lastActivityAt = state.locatorUpdatedAt,
            )
        }
    }

    private class ContinueProvider(private val next: EntryChapter?) : EntryContinueProcessor {
        override val type = EntryType.BOOK
        override suspend fun findNext(entry: Entry): EntryChapter? = next
        override fun open(context: Context, entry: Entry, chapter: EntryChapter) = Unit
    }

    private fun chapter(id: Long) = EntryChapter.create().copy(id = id, entryId = entry.id)
}

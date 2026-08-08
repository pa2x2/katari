package mihon.entry.interactions.navigation

import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.runtime.EntryInteractionPlugin
import mihon.entry.interactions.runtime.EntryInteractionProviderBinding
import mihon.entry.interactions.runtime.createEntryInteractionComposition
import mihon.feature.graph.ContributionOwner
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryProgressState
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.entry.service.EntryChildOwnershipResolution
import tachiyomi.domain.entry.service.EntryChildOwnershipResolutionPort

class EntryContinueFeatureTest {
    private val context = mockk<Context>(relaxed = true)
    private val entry = Entry.create().copy(id = 7L, type = EntryType.BOOK)

    @Test
    fun `an applicable provider reports that no next child exists`() = runTest {
        val feature = featureFor(
            plugin(EntryType.BOOK, EntryContinueCapability.bind(RecordingContinueProcessor(EntryType.BOOK, null))),
        )

        feature.continueEntry(context, entry) shouldBe EntryContinueResult.NoNext
    }

    @Test
    fun `missing Continue provider is valid and exposes no Continue action`() = runTest {
        val feature = featureFor()

        feature.isApplicable(entry.type) shouldBe false
        feature.nextTarget(entry) shouldBe EntryContinueTargetResult.Inapplicable
        feature.continueEntry(context, entry) shouldBe EntryContinueResult.Inapplicable
    }

    @Test
    fun `batch target uses complete child ownership rather than Library membership`() = runTest {
        val libraryEntry = entry.copy(profileId = 3L, favorite = true)
        val nonLibraryOwner = Entry.create().copy(
            id = 8L,
            profileId = libraryEntry.profileId,
            favorite = false,
            type = EntryType.BOOK,
        )
        val libraryChapter = EntryChapter.create().copy(id = 70L, entryId = libraryEntry.id, read = true)
        val nonLibraryChapter = EntryChapter.create().copy(id = 80L, entryId = nonLibraryOwner.id)
        val childOwnership = mockk<EntryChildOwnershipResolutionPort> {
            coEvery { resolveChildOwnership(libraryEntry.profileId, setOf(libraryEntry.id)) } returns mapOf(
                libraryEntry.id to EntryChildOwnershipResolution(
                    profileId = libraryEntry.profileId,
                    requestedEntryId = libraryEntry.id,
                    visibleEntryId = libraryEntry.id,
                    orderedOwners = listOf(libraryEntry, nonLibraryOwner),
                ),
            )
        }
        val chapterRepository = mockk<EntryChapterRepository> {
            every {
                getChaptersByEntryIds(listOf(libraryEntry.id, nonLibraryOwner.id))
            } returns flowOf(listOf(libraryChapter, nonLibraryChapter))
        }
        val progressRepository = mockk<EntryProgressRepository> {
            coEvery { getByEntryIds(setOf(libraryEntry.id, nonLibraryOwner.id)) } returns emptyList()
        }
        val processor = RecordingContinueProcessor(
            type = EntryType.BOOK,
            next = null,
            preparedNext = { chapters -> chapters.first { it.entryId == nonLibraryOwner.id } },
        )
        val feature = featureFor(
            plugin(EntryType.BOOK, EntryContinueCapability.bind(processor)),
            batchPreparation = DefaultEntryContinueBatchPreparation(
                childOwnership,
                chapterRepository,
                progressRepository,
            ),
        )

        feature.nextTargets(listOf(libraryEntry)).getValue(libraryEntry.id) shouldBe
            EntryContinueTargetResult.Available(nonLibraryChapter)
    }

    private fun featureFor(
        vararg plugins: EntryInteractionPlugin,
        batchPreparation: EntryContinueBatchPreparation = mockk(relaxed = true),
    ): EntryContinueFeature {
        val composition = createEntryInteractionComposition(
            plugins = plugins.toList(),
            featureContributors = listOf(EntryContinueFeatureContributor),
        )
        return DefaultEntryContinueFeature(
            evaluation = composition.featureGraphEvaluation,
            interaction = composition.interactions.continueEntry,
            batchPreparation = batchPreparation,
        )
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

    private class RecordingContinueProcessor(
        override val type: EntryType,
        private val next: EntryChapter?,
        private val preparedNext: ((List<EntryChapter>) -> EntryChapter?)? = null,
    ) : EntryContinueProcessor {
        override suspend fun findNext(entry: Entry): EntryChapter? = next

        override suspend fun findNext(
            entry: Entry,
            chapters: List<EntryChapter>,
            progressStates: List<EntryProgressState>,
        ): EntryChapter? = preparedNext?.invoke(chapters) ?: next

        override fun open(context: Context, entry: Entry, chapter: EntryChapter) = Unit
    }
}

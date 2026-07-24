package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mihon.feature.graph.ContributionOwner
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

class EntryBookmarkFeatureTest {
    private val entry = Entry.create().copy(id = 7L, type = EntryType.BOOK)
    private val unbookmarked = EntryChapter.create().copy(id = 11L, entryId = entry.id, bookmark = false)
    private val bookmarked = EntryChapter.create().copy(id = 12L, entryId = entry.id, bookmark = true)

    @Test
    fun `provider absence is valid and returns structured inapplicability`() = runTest {
        val feature = featureFor()
        val target = EntryBookmarkTarget(entry.type, unbookmarked.bookmarkStatus())

        feature.isApplicable(entry.type) shouldBe false
        feature.availability(target, bookmarked = true) shouldBe
            EntryBookmarkAvailability.Inapplicable(setOf(entry.type))
        feature.setBookmarked(entry, listOf(unbookmarked), bookmarked = true) shouldBe
            EntryBookmarkMutationResult.Inapplicable(entry.type)
    }

    @Test
    fun `selection availability distinguishes state changes from no-ops`() {
        val processor = mockk<EntryBookmarkProcessor> {
            every { type } returns EntryType.BOOK
        }
        val feature = featureFor(EntryBookmarkCapability.bind(processor))
        val unbookmarkedTarget = EntryBookmarkTarget(entry.type, unbookmarked.bookmarkStatus())
        val bookmarkedTarget = EntryBookmarkTarget(entry.type, bookmarked.bookmarkStatus())

        feature.selectionAvailability(listOf(bookmarkedTarget, unbookmarkedTarget), bookmarked = true) shouldBe
            EntryBookmarkAvailability.Available
        feature.selectionAvailability(listOf(bookmarkedTarget), bookmarked = true) shouldBe
            EntryBookmarkAvailability.NoChange
        feature.selectionAvailability(listOf(bookmarkedTarget), bookmarked = false) shouldBe
            EntryBookmarkAvailability.Available
        feature.selectionAvailability(listOf(bookmarkedTarget, unbookmarkedTarget), bookmarked = false) shouldBe
            EntryBookmarkAvailability.NoChange
    }

    private fun featureFor(
        vararg bindings: EntryInteractionProviderBinding<*>,
    ): EntryBookmarkFeature {
        val composition = createEntryInteractionComposition(
            plugins = listOf(plugin(*bindings)),
            featureContributors = listOf(EntryBookmarkFeatureContributor),
        )
        return DefaultEntryBookmarkFeature(
            evaluation = composition.featureGraphEvaluation,
            interaction = composition.interactions.bookmark,
        )
    }

    private fun plugin(vararg bindings: EntryInteractionProviderBinding<*>): EntryInteractionPlugin {
        return object : EntryInteractionPlugin {
            override val type = EntryType.BOOK
            override val owner = ContributionOwner("test.type.book")
            override val providerBindings = bindings.toList()
        }
    }
}

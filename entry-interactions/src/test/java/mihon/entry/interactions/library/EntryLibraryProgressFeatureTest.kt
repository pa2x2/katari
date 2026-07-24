package mihon.entry.interactions

import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import mihon.feature.graph.ContributionOwner
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.service.EntryLibraryContinueTarget
import tachiyomi.domain.entry.service.EntryLibraryProgressResolution
import tachiyomi.domain.entry.service.EntryLibraryProgressSummary

class EntryLibraryProgressFeatureTest {
    private val entry = Entry.create().copy(id = 7L, type = EntryType.BOOK)

    @Test
    fun `missing provider keeps summary inapplicable without hiding structural entry state`() = runTest {
        val feature = features(plugin(EntryType.BOOK)).libraryProgress

        feature.isApplicable(entry.type) shouldBe false
        feature.calculate(entry, emptyList(), 10L) shouldBe EntryLibraryProgressResolution.Inapplicable(entry.type)
    }

    @Test
    fun `provider activates shared counts Continue and Bookmark behaviors`() = runTest {
        val next = chapter(id = 12L)
        val features = features(
            plugin(
                EntryType.BOOK,
                EntryLibraryProgressCapability.bind(
                    EvidenceProvider(
                        evidence = EntryLibraryProgressEvidence(
                            hasMediaProgress = true,
                            inProgressItemId = 11L,
                            inProgressFraction = 0.5f,
                            lastActivityAt = 30L,
                        ),
                    ),
                ),
                EntryContinueCapability.bind(ContinueProvider(next)),
                EntryBookmarkCapability.bind(BookmarkProvider()),
            ),
        )

        val result = features.libraryProgress.calculate(
            entry = entry,
            chapters = listOf(chapter(id = 11L, read = true, bookmark = true), next),
            lastRead = 20L,
        ).shouldBeInstanceOf<EntryLibraryProgressResolution.Available>().summary

        result.totalCount shouldBe 2L
        result.consumedCount shouldBe 1L
        result.hasStarted shouldBe true
        result.bookmarkCount shouldBe 1L
        result.inProgressItemId shouldBe 11L
        result.inProgressFraction shouldBe 0.5f
        result.lastRead shouldBe 30L
        result.continueTarget shouldBe EntryLibraryContinueTarget.Available(12L)
    }

    @Test
    fun `shared merge aggregates summaries and retains structured relationship absence`() {
        val feature = features(
            plugin(
                EntryType.BOOK,
                EntryLibraryProgressCapability.bind(EvidenceProvider()),
            ),
        ).libraryProgress

        val result = feature.merge(
            EntryType.BOOK,
            listOf(
                summary(total = 3L, consumed = 1L, lastRead = 10L, inProgress = 2L),
                summary(total = 4L, consumed = 2L, lastRead = 20L, inProgress = null),
            ),
        ).shouldBeInstanceOf<EntryLibraryProgressResolution.Available>().summary

        result.totalCount shouldBe 7L
        result.consumedCount shouldBe 3L
        result.lastRead shouldBe 20L
        result.inProgressItemId shouldBe 2L
        result.bookmarkCount shouldBe null
        result.continueTarget shouldBe EntryLibraryContinueTarget.Inapplicable
    }

    private fun features(vararg plugins: EntryInteractionPlugin): Features {
        val composition = createEntryInteractionComposition(
            plugins = plugins.toList(),
            featureContributors = listOf(
                EntryContinueFeatureContributor,
                EntryLibraryProgressFeatureContributor,
            ),
        )
        val continueFeature = DefaultEntryContinueFeature(
            evaluation = composition.featureGraphEvaluation,
            interaction = composition.interactions.continueEntry,
        )
        return Features(
            libraryProgress = DefaultEntryLibraryProgressFeature(
                evaluation = composition.featureGraphEvaluation,
                interaction = composition.interactions.libraryProgress,
                continueFeature = continueFeature,
            ),
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

    private class EvidenceProvider(
        private val evidence: EntryLibraryProgressEvidence = EntryLibraryProgressEvidence(false, null, null, 0L),
    ) : EntryLibraryProgressProvider {
        override val type = EntryType.BOOK
        override suspend fun evidence(entry: Entry, chapters: List<EntryChapter>) = evidence
    }

    private class ContinueProvider(private val next: EntryChapter?) : EntryContinueProcessor {
        override val type = EntryType.BOOK
        override suspend fun findNext(entry: Entry): EntryChapter? = next
        override fun open(context: Context, entry: Entry, chapter: EntryChapter) = Unit
    }

    private class BookmarkProvider : EntryBookmarkProcessor {
        override val type = EntryType.BOOK
        override suspend fun setBookmarked(entry: Entry, chapters: List<EntryChapter>, bookmarked: Boolean) = Unit
    }

    private fun chapter(id: Long, read: Boolean = false, bookmark: Boolean = false) =
        EntryChapter.create().copy(id = id, entryId = entry.id, read = read, bookmark = bookmark)

    private fun summary(
        total: Long,
        consumed: Long,
        lastRead: Long,
        inProgress: Long?,
    ) = EntryLibraryProgressSummary(
        totalCount = total,
        consumedCount = consumed,
        hasStarted = consumed > 0L || inProgress != null,
        bookmarkCount = null,
        inProgressItemId = inProgress,
        inProgressFraction = inProgress?.let { 0.5f },
        lastRead = lastRead,
        continueTarget = EntryLibraryContinueTarget.Inapplicable,
    )

    private data class Features(
        val libraryProgress: EntryLibraryProgressFeature,
    )
}

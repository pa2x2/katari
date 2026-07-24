package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mihon.feature.graph.ContributionOwner
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.i18n.MR

class EntryChildListFeatureTest {
    private val entry = Entry.create().copy(id = 7L, type = EntryType.BOOK)
    private val first = EntryChapter.create().copy(id = 11L, entryId = entry.id)
    private val second = EntryChapter.create().copy(id = 12L, entryId = entry.id)

    @Test
    fun `provider absence is valid and returns structured inapplicability`() = runTest {
        val feature = featureFor()
        val request = request()

        feature.isApplicable(entry.type) shouldBe false
        feature.readingOrder(entry, request.chapters, request.memberIds) shouldBe
            EntryChildOrderResult.Inapplicable(entry.type)
        feature.firstReadingChild(entry, request.chapters, request.memberIds) shouldBe
            EntryFirstChildResult.Inapplicable(entry.type)
        feature.displayOrder(entry, request.chapters, request.memberIds) shouldBe
            EntryChildOrderResult.Inapplicable(entry.type)
        feature.displayList(request) shouldBe EntryChildListResult.Inapplicable(entry.type)
        feature.progressLabels(progressRequest()) shouldBe EntryChildProgressResult.Inapplicable(entry.type)
    }

    @Test
    fun `child-list provider builds display rows without implying progress labels`() = runTest {
        val feature = featureFor(EntryChildListCapability.bind(RecordingChildListProcessor()))

        feature.displayList(request()) shouldBe EntryChildListResult.Available(
            EntryChildListDisplay(
                rows = listOf(EntryChildListRow.Child(first), EntryChildListRow.Child(second)),
                aggregateMissingCount = 0,
            ),
        )
        feature.progressLabels(progressRequest()) shouldBe EntryChildProgressResult.Inapplicable(entry.type)
    }

    @Test
    fun `child-progress provider alone does not manufacture child-list behavior`() = runTest {
        val feature = featureFor(EntryChildProgressCapability.bind(RecordingChildProgressProcessor()))

        feature.isApplicable(entry.type) shouldBe false
        feature.progressLabels(progressRequest()) shouldBe EntryChildProgressResult.Inapplicable(entry.type)
    }

    @Test
    fun `independent child-list and child-progress providers activate progress labels together`() = runTest {
        val feature = featureFor(
            EntryChildListCapability.bind(RecordingChildListProcessor()),
            EntryChildProgressCapability.bind(RecordingChildProgressProcessor()),
        )

        val result = feature.progressLabels(progressRequest()) as EntryChildProgressResult.Available
        result.labels.first() shouldBe mapOf(first.id to EntryChildProgressLabel(resource = MR.strings.label_started))
    }

    @Test
    fun `missing-gap provider owns missing rows and counts`() {
        val processor = RecordingMissingGapProcessor()
        val feature = featureFor(
            EntryChildListCapability.bind(processor),
            EntryMissingChildGapCapability.bind(processor),
        )

        feature.displayList(request()) shouldBe EntryChildListResult.Available(
            EntryChildListDisplay(
                rows = listOf(EntryChildListRow.MissingCount("gap", 3)),
                aggregateMissingCount = 3,
            ),
        )
    }

    private fun featureFor(
        vararg bindings: EntryInteractionProviderBinding<*>,
    ): EntryChildListFeature {
        val composition = createEntryInteractionComposition(
            plugins = listOf(plugin(*bindings)),
            featureContributors = listOf(EntryChildListFeatureContributor),
        )
        return DefaultEntryChildListFeature(
            evaluation = composition.featureGraphEvaluation,
            childList = composition.interactions.childList,
            childProgress = composition.interactions.childProgress,
            missingChildGap = composition.interactions.missingChildGap,
        )
    }

    private fun plugin(vararg bindings: EntryInteractionProviderBinding<*>): EntryInteractionPlugin {
        return object : EntryInteractionPlugin {
            override val type = EntryType.BOOK
            override val owner = ContributionOwner("test.type.book")
            override val providerBindings = bindings.toList()
        }
    }

    private fun request() = EntryChildListRequest(
        entry = entry,
        chapters = listOf(first, second),
        memberIds = listOf(entry.id),
    )

    private fun progressRequest() = EntryChildProgressRequest(
        entry = entry,
        chapters = listOf(first, second),
        memberIds = listOf(entry.id),
    )

    private class RecordingChildListProcessor : EntryChildListProcessor {
        override val type = EntryType.BOOK

        override fun sortedForReading(
            entry: Entry,
            chapters: List<EntryChapter>,
            memberIds: List<Long>,
        ): List<EntryChapter> = chapters.reversed()

        override fun sortedForDisplay(
            entry: Entry,
            chapters: List<EntryChapter>,
            memberIds: List<Long>,
        ): List<EntryChapter> = chapters
    }

    private class RecordingMissingGapProcessor : EntryChildListProcessor, EntryMissingChildGapProcessor {
        override val type = EntryType.BOOK

        override fun sortedForReading(
            entry: Entry,
            chapters: List<EntryChapter>,
            memberIds: List<Long>,
        ) = chapters

        override fun sortedForDisplay(
            entry: Entry,
            chapters: List<EntryChapter>,
            memberIds: List<Long>,
        ) = chapters

        override fun buildDisplayList(request: EntryChildListRequest) = EntryChildListDisplay(
            rows = listOf(EntryChildListRow.MissingCount("gap", 3)),
            aggregateMissingCount = 3,
        )
    }

    private class RecordingChildProgressProcessor : EntryChildProgressProcessor {
        override val type = EntryType.BOOK

        override fun progressLabels(
            request: EntryChildProgressRequest,
        ): Flow<Map<Long, EntryChildProgressLabel>> {
            return flowOf(mapOf(request.chapters.first().id to EntryChildProgressLabel(MR.strings.label_started)))
        }
    }
}

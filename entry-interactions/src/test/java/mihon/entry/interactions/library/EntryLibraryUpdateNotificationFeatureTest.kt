package mihon.entry.interactions.library

import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.source.entry.UnmeteredSource
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.presentation.EntryTypePresentationFeature
import mihon.entry.interactions.presentation.EntryTypePresentationResult
import mihon.entry.interactions.presentation.genericEntryTypePresentation
import mihon.entry.interactions.runtime.EntryInteractionPlugin
import mihon.entry.interactions.runtime.EntryTypePresentationCapability
import mihon.entry.interactions.runtime.EntryTypePresentationProvider
import mihon.entry.interactions.runtime.createEntryInteractionComposition
import mihon.feature.graph.ContributionOwner
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.source.service.SourceManager

class EntryLibraryUpdateNotificationFeatureTest {
    private val entry = Entry.create().copy(id = 7L, source = 11L, type = EntryType.BOOK)
    private val child = EntryChapter.create().copy(
        id = 12L,
        entryId = entry.id,
        chapterNumber = 2.5,
    )

    @Test
    fun `update description lists only recognized child numbers`() = runTest {
        val feature = featureFor()
        val updates = listOf(
            EntryLibraryUpdateNotificationInput(
                entry,
                listOf(
                    child.copy(id = 1L, chapterNumber = 1.0),
                    child.copy(id = 2L, chapterNumber = 2.5),
                    child.copy(id = 3L, chapterNumber = -1.0),
                ),
            ),
        )

        val text = feature.project(updates).groups.single().updates.single().description
            .shouldBeInstanceOf<EntryLibraryUpdateNotificationText.StringText>()

        text.resource shouldBe genericEntryTypePresentation.updateNotification.childMultiple
        text.arguments shouldBe listOf("1, 2.5")
    }

    @Test
    fun `queue warning counts only metered sources against the threshold`() {
        val unmetered = mockk<UnifiedSource>(moreInterfaces = arrayOf(UnmeteredSource::class))
        val metered = mockk<UnifiedSource>()
        val sourceManager = mockk<SourceManager> {
            every { get(1L) } returns unmetered
            every { get(2L) } returns metered
        }
        val feature = featureFor(sourceManager = sourceManager, queueWarningThreshold = 2)
        val entries = listOf(
            entry.copy(id = 1L, source = 1L),
            entry.copy(id = 2L, source = 1L),
            entry.copy(id = 3L, source = 1L),
            entry.copy(id = 4L, source = 2L),
            entry.copy(id = 5L, source = 2L),
        )

        feature.queueWarning(entries) shouldBe EntryLibraryUpdateQueueWarning.NotRequired
        feature.queueWarning(entries + entry.copy(id = 6L, source = 2L)) shouldBe
            EntryLibraryUpdateQueueWarning.Required(maxEntriesPerMeteredSource = 3)
    }

    private fun featureFor(
        sourceManager: SourceManager = mockk(relaxed = true),
        queueWarningThreshold: Int = 60,
    ): EntryLibraryUpdateNotificationFeature {
        val plugin = object : EntryInteractionPlugin {
            override val type = EntryType.BOOK
            override val owner = ContributionOwner("test.notification.book")
            override val providerBindings = listOf(
                EntryTypePresentationCapability.bind(
                    object : EntryTypePresentationProvider {
                        override val type = EntryType.BOOK
                        override val presentation = genericEntryTypePresentation
                    },
                ),
            )
        }
        val composition = createEntryInteractionComposition(
            plugins = listOf(plugin),
            featureContributors = listOf(EntryLibraryUpdateNotificationFeatureContributor),
        )
        val presentationFeature = mockk<EntryTypePresentationFeature> {
            every { presentation(EntryType.BOOK) } returns
                EntryTypePresentationResult.Contributed(EntryType.BOOK, genericEntryTypePresentation)
        }
        return DefaultEntryLibraryUpdateNotificationFeature(
            evaluation = composition.featureGraphEvaluation,
            presentationFeature = presentationFeature,
            openFeature = mockk(),
            consumptionFeature = mockk(),
            downloadActionFeature = mockk(),
            sourceManager = sourceManager,
            resolveVisibleEntry = { it },
            queueWarningThreshold = queueWarningThreshold,
        )
    }
}

package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.source.entry.UnmeteredSource
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
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
    fun `a partial type without presentation or actions receives explicit generic notification behavior`() = runTest {
        val feature = featureFor(plugin(EntryType.BOOK))

        val group = feature.project(listOf(input())).groups.single()
        val item = group.updates.single()

        group.route.channelLabel shouldBe genericEntryTypePresentation.updateNotification.channelLabel
        group.summaryTitle shouldBe genericEntryTypePresentation.updateNotification.summaryTitle
        item.destination shouldBe EntryLibraryUpdateNotificationDestination.ENTRY_DETAILS
        item.actions.shouldContainExactly(EntryLibraryUpdateNotificationAction.VIEW_ENTRY)
    }

    @Test
    fun `missing action relationships retain notification participation with explicit details destination`() = runTest {
        val feature = featureFor(plugin(EntryType.BOOK, presentationBinding(EntryType.BOOK)))

        val group = feature.project(listOf(input())).groups.single()
        val item = group.updates.single()

        item.destination shouldBe EntryLibraryUpdateNotificationDestination.ENTRY_DETAILS
        item.actions.shouldContainExactly(EntryLibraryUpdateNotificationAction.VIEW_ENTRY)
        group.route.channelId shouldBe "entry_library_updates_book_channel"
        group.route.groupKey shouldBe "mihon.entry.library_updates.book"
        group.route.summaryNotificationId shouldBe -18739594
    }

    @Test
    fun `independent capability relationships derive only their shared notification actions`() = runTest {
        val feature = featureFor(
            plugin(
                EntryType.BOOK,
                presentationBinding(EntryType.BOOK),
                EntryOpenCapability.bind(openProvider()),
                EntryConsumptionCapability.bind(consumptionProvider()),
                EntryDownloadCapability.bind(downloadProvider()),
            ),
            downloadAvailability = EntryDownloadActionAvailability.Available,
        )

        val item = feature.project(listOf(input())).groups.single().updates.single()

        item.destination shouldBe EntryLibraryUpdateNotificationDestination.OPEN_CHILD
        item.actions shouldBe setOf(
            EntryLibraryUpdateNotificationAction.VIEW_ENTRY,
            EntryLibraryUpdateNotificationAction.MARK_CONSUMED,
            EntryLibraryUpdateNotificationAction.DOWNLOAD,
        )
    }

    @Test
    fun `Open alone changes the destination without adding unrelated actions`() = runTest {
        val feature = featureFor(
            plugin(EntryType.BOOK, EntryOpenCapability.bind(openProvider())),
        )

        val item = feature.project(listOf(input())).groups.single().updates.single()

        item.destination shouldBe EntryLibraryUpdateNotificationDestination.OPEN_CHILD
        item.actions.shouldContainExactly(EntryLibraryUpdateNotificationAction.VIEW_ENTRY)
    }

    @Test
    fun `Consumption alone adds only Mark Consumed`() = runTest {
        val feature = featureFor(
            plugin(EntryType.BOOK, EntryConsumptionCapability.bind(consumptionProvider())),
        )

        val item = feature.project(listOf(input())).groups.single().updates.single()

        item.destination shouldBe EntryLibraryUpdateNotificationDestination.ENTRY_DETAILS
        item.actions shouldBe setOf(
            EntryLibraryUpdateNotificationAction.VIEW_ENTRY,
            EntryLibraryUpdateNotificationAction.MARK_CONSUMED,
        )
    }

    @Test
    fun `empty update context blocks child actions without removing the notification`() = runTest {
        val feature = featureFor(
            plugin(
                EntryType.BOOK,
                EntryOpenCapability.bind(openProvider()),
                EntryConsumptionCapability.bind(consumptionProvider()),
            ),
        )

        val item = feature.project(listOf(input(children = emptyList()))).groups.single().updates.single()

        item.destination shouldBe EntryLibraryUpdateNotificationDestination.ENTRY_DETAILS
        item.actions.shouldContainExactly(EntryLibraryUpdateNotificationAction.VIEW_ENTRY)
    }

    @Test
    fun `available Download alone adds only Download`() = runTest {
        val feature = featureFor(
            plugin(EntryType.BOOK, EntryDownloadCapability.bind(downloadProvider())),
            downloadAvailability = EntryDownloadActionAvailability.Available,
        )

        val item = feature.project(listOf(input())).groups.single().updates.single()

        item.destination shouldBe EntryLibraryUpdateNotificationDestination.ENTRY_DETAILS
        item.actions shouldBe setOf(
            EntryLibraryUpdateNotificationAction.VIEW_ENTRY,
            EntryLibraryUpdateNotificationAction.DOWNLOAD,
        )
    }

    @Test
    fun `contextual download rejection omits only the download action`() = runTest {
        val feature = featureFor(
            plugin(
                EntryType.BOOK,
                presentationBinding(EntryType.BOOK),
                EntryDownloadCapability.bind(downloadProvider()),
            ),
            downloadAvailability = EntryDownloadActionAvailability.Blocked(
                setOf(EntryDownloadActionBlocker.LOCAL_OR_STUB),
            ),
        )

        val item = feature.project(
            listOf(input()),
        ).groups.single().updates.single()

        item.actions.shouldContainExactly(EntryLibraryUpdateNotificationAction.VIEW_ENTRY)
    }

    @Test
    fun `shared description policy produces vocabulary-ready text without media branches`() = runTest {
        val feature = featureFor(plugin(EntryType.BOOK, presentationBinding(EntryType.BOOK)))
        val updates = listOf(
            input(
                children = listOf(
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
    fun `merged visible identity is retained for a same-type Open destination`() = runTest {
        val visibleEntry = entry.copy(id = 99L)
        val feature = featureFor(
            plugin(EntryType.BOOK, EntryOpenCapability.bind(openProvider())),
            resolveVisibleEntry = { visibleEntry },
        )

        val item = feature.project(listOf(input())).groups.single().updates.single()

        item.originEntry shouldBe entry
        item.visibleEntry shouldBe visibleEntry
        item.destination shouldBe EntryLibraryUpdateNotificationDestination.OPEN_CHILD
    }

    @Test
    fun `a merged visible target with another type fails the same-type invariant`() = runTest {
        val feature = featureFor(
            plugin(EntryType.BOOK),
            resolveVisibleEntry = { it.copy(id = 99L, type = EntryType.ANIME) },
        )

        runCatching { feature.project(listOf(input())) }.exceptionOrNull()
            .shouldBeInstanceOf<IllegalStateException>()
    }

    @Test
    fun `legacy routes are frozen while Book uses derived neutral routing`() {
        val feature = featureFor(
            plugin(EntryType.MANGA, presentationBinding(EntryType.MANGA)),
            plugin(EntryType.ANIME, presentationBinding(EntryType.ANIME)),
            plugin(EntryType.BOOK, presentationBinding(EntryType.BOOK)),
        )

        feature.routes().associateBy { it.type }.let { routes ->
            routes.getValue(EntryType.MANGA).summaryNotificationId shouldBe -301
            routes.getValue(EntryType.MANGA).channelId shouldBe "new_chapters_channel"
            routes.getValue(EntryType.MANGA).groupKey shouldBe "eu.kanade.tachiyomi.NEW_CHAPTERS"
            routes.getValue(EntryType.ANIME).summaryNotificationId shouldBe -302
            routes.getValue(EntryType.ANIME).channelId shouldBe "new_episodes_channel"
            routes.getValue(EntryType.ANIME).groupKey shouldBe "eu.kanade.tachiyomi.NEW_EPISODES"
            routes.getValue(EntryType.BOOK).channelId shouldBe "entry_library_updates_book_channel"
        }
    }

    @Test
    fun `queue warning excludes unmetered sources and applies shared threshold`() {
        val unmetered = mockk<UnifiedSource>(moreInterfaces = arrayOf(UnmeteredSource::class))
        val metered = mockk<UnifiedSource>()
        val sourceManager = mockk<SourceManager> {
            every { get(1L) } returns unmetered
            every { get(2L) } returns metered
        }
        val feature = featureFor(
            plugin(EntryType.BOOK),
            sourceManager = sourceManager,
            queueWarningThreshold = 2,
        )
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
        vararg plugins: EntryInteractionPlugin,
        downloadAvailability: EntryDownloadActionAvailability = EntryDownloadActionAvailability.Inapplicable(
            setOf(EntryType.BOOK),
        ),
        resolveVisibleEntry: suspend (Entry) -> Entry = { it },
        sourceManager: SourceManager = mockk(relaxed = true),
        queueWarningThreshold: Int = 60,
    ): EntryLibraryUpdateNotificationFeature {
        val composition = createEntryInteractionComposition(
            plugins = plugins.toList(),
            featureContributors = listOf(EntryLibraryUpdateNotificationFeatureContributor),
        )
        val presentationTypes = providerTypes<EntryTypePresentationProvider>(plugins)
        val openTypes = providerTypes<EntryOpenProcessor>(plugins)
        val consumptionTypes = providerTypes<EntryConsumptionProcessor>(plugins)
        val presentationFeature = mockk<EntryTypePresentationFeature> {
            every { genericPresentation } returns genericEntryTypePresentation
            every { presentation(any()) } answers {
                val type = firstArg<EntryType?>()
                if (type != null && type in presentationTypes) {
                    EntryTypePresentationResult.Contributed(type, genericEntryTypePresentation)
                } else {
                    EntryTypePresentationResult.Generic(type, genericEntryTypePresentation)
                }
            }
        }
        val openFeature = mockk<EntryOpenFeature> {
            every { isApplicable(any()) } answers { firstArg<EntryType>() in openTypes }
        }
        val consumptionFeature = mockk<EntryConsumptionFeature> {
            every { isApplicable(any()) } answers { firstArg<EntryType>() in consumptionTypes }
        }
        val downloadFeature = mockk<EntryDownloadActionFeature> {
            every { notificationAvailability(any(), any()) } returns downloadAvailability
        }
        return DefaultEntryLibraryUpdateNotificationFeature(
            evaluation = composition.featureGraphEvaluation,
            presentationFeature = presentationFeature,
            openFeature = openFeature,
            consumptionFeature = consumptionFeature,
            downloadActionFeature = downloadFeature,
            sourceManager = sourceManager,
            resolveVisibleEntry = resolveVisibleEntry,
            queueWarningThreshold = queueWarningThreshold,
        )
    }

    private inline fun <reified P : EntryInteractionProvider> providerTypes(
        plugins: Array<out EntryInteractionPlugin>,
    ): Set<EntryType> {
        return plugins.mapNotNullTo(mutableSetOf()) { plugin ->
            plugin.type.takeIf {
                plugin.providerBindings.any { binding -> binding.implementation is P }
            }
        }
    }

    private fun plugin(
        type: EntryType,
        vararg bindings: EntryInteractionProviderBinding<*>,
    ): EntryInteractionPlugin {
        return object : EntryInteractionPlugin {
            override val type = type
            override val owner = ContributionOwner("test.notification.${type.name.lowercase()}")
            override val providerBindings = bindings.toList()
        }
    }

    private fun presentationBinding(type: EntryType): EntryInteractionProviderBinding<EntryTypePresentationProvider> {
        return EntryTypePresentationCapability.bind(
            object : EntryTypePresentationProvider {
                override val type = type
                override val presentation = genericEntryTypePresentation
            },
        )
    }

    private fun openProvider(): EntryOpenProcessor {
        return mockk(relaxed = true) {
            every { type } returns EntryType.BOOK
        }
    }

    private fun consumptionProvider(): EntryConsumptionProcessor {
        return mockk(relaxed = true) {
            every { type } returns EntryType.BOOK
        }
    }

    private fun downloadProvider(): EntryDownloadProcessor {
        return mockk(relaxed = true) {
            every { type } returns EntryType.BOOK
        }
    }

    private fun input(
        children: List<EntryChapter> = listOf(child),
    ): EntryLibraryUpdateNotificationInput {
        return EntryLibraryUpdateNotificationInput(entry, children)
    }
}

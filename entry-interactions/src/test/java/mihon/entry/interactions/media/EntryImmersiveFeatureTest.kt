package mihon.entry.interactions

import android.app.PendingIntent
import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryCatalogueSource
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.SourceMetadata
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mihon.feature.graph.ContributionOwner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

class EntryImmersiveFeatureTest {
    private val context = mockk<Context>(relaxed = true)
    private val entry = Entry.create().copy(id = 1L, type = EntryType.BOOK)
    private val first = EntryChapter.create().copy(id = 11L, entryId = entry.id)
    private val second = EntryChapter.create().copy(id = 12L, entryId = entry.id)
    private val source = catalogueSource(optedIn = true)

    @Test
    fun `provider absence is valid and returns structured inapplicability`() = runTest {
        val feature = featureFor()

        feature.sourceAvailability(source) shouldBe EntryImmersiveSourceAvailability.NoRuntimeType
        feature.availability(EntryImmersiveContext(entry, source)) shouldBe
            EntryImmersiveAvailability.Inapplicable(entry.type)
        feature.preloadRadius(entry.type) shouldBe EntryImmersivePreloadRadiusResult.Inapplicable(entry.type)
        feature.load(request()).shouldBeInstanceOf<EntryImmersiveLoadResult.Inapplicable>()
    }

    @Test
    fun `entry level provider with zero preload activates every common lifecycle behavior`() = runTest {
        val processor = RecordingImmersiveProcessor(preloadRadius = 0)
        val feature = featureFor(EntryImmersiveCapability.bind(processor))

        feature.availability(EntryImmersiveContext(entry, source)) shouldBe
            EntryImmersiveAvailability.Available(
                preloadRadius = 0,
                childRequirement = EntryImmersiveChildRequirement.NONE,
            )
        feature.preloadRadius(entry.type) shouldBe EntryImmersivePreloadRadiusResult.Available(radius = 0)
        val loaded = feature.load(request()).shouldBeInstanceOf<EntryImmersiveLoadResult.Loaded>()
        loaded.child shouldBe null
        feature.renderer(loaded.handle) shouldBe EntryImmersiveRendererResult.Available(processor.renderer)
        feature.persistProgress(loaded.handle, EntryImmersiveProgress.ImagePage(0, 1, 0L))
        feature.release(loaded.handle)

        processor.loadedChild shouldBe null
        processor.progressCount shouldBe 1
        processor.releaseCount shouldBe 1
        assertThrows<IllegalStateException> {
            feature.release(
                EntryImmersiveHandle.ImagePages(EntryType.MANGA, chapterId = null, delegate = Unit),
            )
        }
    }

    @Test
    fun `source blockers remain distinct from provider absence and media failure`() = runTest {
        val processor = RecordingImmersiveProcessor(failure = IllegalStateException("media failed"))
        val feature = featureFor(EntryImmersiveCapability.bind(processor))

        feature.availability(EntryImmersiveContext(entry, null)) shouldBe
            EntryImmersiveAvailability.ContextuallyUnavailable(EntryImmersiveUnavailableReason.SourceUnavailable)
        feature.load(request(source = null)) shouldBe EntryImmersiveLoadResult.ContextuallyUnavailable(
            EntryImmersiveUnavailableReason.SourceUnavailable,
        )

        val optedOut = catalogueSource(optedIn = false)
        feature.availability(EntryImmersiveContext(entry, optedOut)) shouldBe
            EntryImmersiveAvailability.ContextuallyUnavailable(EntryImmersiveUnavailableReason.SourceOptedOut)
        feature.load(request(source = optedOut)) shouldBe EntryImmersiveLoadResult.ContextuallyUnavailable(
            EntryImmersiveUnavailableReason.SourceOptedOut,
        )

        feature.load(request()).shouldBeInstanceOf<EntryImmersiveLoadResult.Failed>().error.message shouldBe
            "media failed"
    }

    @Test
    fun `renderer failures remain structured inside the Immersive Feature`() = runTest {
        val processor = RecordingImmersiveProcessor(rendererFailure = IllegalStateException("renderer failed"))
        val feature = featureFor(EntryImmersiveCapability.bind(processor))
        val loaded = feature.load(request()).shouldBeInstanceOf<EntryImmersiveLoadResult.Loaded>()

        feature.renderer(loaded.handle).shouldBeInstanceOf<EntryImmersiveRendererResult.Failed>().error.message shouldBe
            "renderer failed"
    }

    @Test
    fun `declared source types only prune the source surface and never reject returned entry type`() {
        val feature = featureFor(EntryImmersiveCapability.bind(RecordingImmersiveProcessor()))
        val declaredOtherType = catalogueSource(optedIn = true, declaredTypes = setOf(EntryType.MANGA))

        feature.sourceAvailability(declaredOtherType) shouldBe
            EntryImmersiveSourceAvailability.NoCompatibleDeclaredType(setOf(EntryType.MANGA))
        feature.availability(EntryImmersiveContext(entry, declaredOtherType)) shouldBe
            EntryImmersiveAvailability.Available(
                preloadRadius = 2,
                childRequirement = EntryImmersiveChildRequirement.NONE,
            )
    }

    @Test
    fun `child backed provider without Child List fails with actionable architecture error`() {
        val error = assertThrows<IllegalStateException> {
            featureFor(
                EntryImmersiveCapability.bind(
                    RecordingImmersiveProcessor(loadMode = EntryImmersiveLoadMode.FIRST_READING_CHILD),
                ),
            )
        }

        error.message shouldBe
            "Child-backed Immersive providers require the Immersive + Child List relationship; " +
            "missing EntryChildList providers for [BOOK]"
    }

    @Test
    fun `Child List and Open relationships activate first child and open target automatically`() = runTest {
        val processor = RecordingImmersiveProcessor(loadMode = EntryImmersiveLoadMode.FIRST_READING_CHILD)
        val feature = featureFor(
            EntryImmersiveCapability.bind(processor),
            EntryChildListCapability.bind(ReverseChildListProcessor()),
            EntryOpenCapability.bind(RecordingOpenProcessor()),
        )

        val loaded = feature.load(request(children = listOf(first, second)))
            .shouldBeInstanceOf<EntryImmersiveLoadResult.Loaded>()
        processor.loadedChild shouldBe second
        loaded.child shouldBe second
        feature.openTarget(loaded.handle) shouldBe EntryImmersiveOpenTargetResult.Available(second.id)
    }

    @Test
    fun `child backed provider reports no reading child separately`() = runTest {
        val feature = featureFor(
            EntryImmersiveCapability.bind(
                RecordingImmersiveProcessor(loadMode = EntryImmersiveLoadMode.FIRST_READING_CHILD),
            ),
            EntryChildListCapability.bind(ReverseChildListProcessor()),
        )

        feature.load(request()) shouldBe EntryImmersiveLoadResult.ContextuallyUnavailable(
            EntryImmersiveUnavailableReason.NoReadingChild,
        )
    }

    @Test
    fun `child backed refresh maps Source Refresh outcomes inside Immersive`() = runTest {
        val sourceRefresh = mockk<EntrySourceRefreshFeature>()
        val feature = featureFor(
            EntryImmersiveCapability.bind(
                RecordingImmersiveProcessor(loadMode = EntryImmersiveLoadMode.FIRST_READING_CHILD),
            ),
            EntryChildListCapability.bind(ReverseChildListProcessor()),
            sourceRefresh = sourceRefresh,
        )

        coEvery { sourceRefresh.refresh(any()) } returns refreshed()
        feature.refreshChildren(entry) shouldBe EntryImmersiveChildRefreshResult.Refreshed

        coEvery { sourceRefresh.refresh(any()) } returns EntrySourceRefreshResult.SourceUnavailable(entry.source)
        feature.refreshChildren(entry) shouldBe EntryImmersiveChildRefreshResult.ContextuallyUnavailable(
            EntryImmersiveUnavailableReason.SourceUnavailable,
        )

        coEvery { sourceRefresh.refresh(any()) } returns
            EntrySourceRefreshResult.Failed(EntrySourceRefreshFailure.NoChildren)
        feature.refreshChildren(entry) shouldBe EntryImmersiveChildRefreshResult.ContextuallyUnavailable(
            EntryImmersiveUnavailableReason.NoReadingChild,
        )

        val failure = IllegalStateException("refresh failed")
        coEvery { sourceRefresh.refresh(any()) } returns
            EntrySourceRefreshResult.Failed(EntrySourceRefreshFailure.Operation(failure))
        feature.refreshChildren(entry) shouldBe EntryImmersiveChildRefreshResult.Failed(failure)
    }

    private fun featureFor(
        vararg bindings: EntryInteractionProviderBinding<*>,
        sourceRefresh: EntrySourceRefreshFeature = mockk(),
    ): EntryImmersiveFeature {
        val composition = createEntryInteractionComposition(
            plugins = listOf(plugin(*bindings)),
            featureContributors = listOf(EntryImmersiveFeatureContributor, EntryChildListFeatureContributor),
        )
        val childList = DefaultEntryChildListFeature(
            evaluation = composition.featureGraphEvaluation,
            childList = composition.interactions.childList,
            childProgress = composition.interactions.childProgress,
            missingChildGap = composition.interactions.missingChildGap,
        )
        return DefaultEntryImmersiveFeature(
            evaluation = composition.featureGraphEvaluation,
            interaction = composition.interactions.immersive,
            childList = childList,
            sourceRefresh = sourceRefresh,
        )
    }

    private fun refreshed() = EntrySourceRefreshResult.Refreshed(
        insertedChildren = emptyList(),
        insertedChildrenTotal = 0,
        updatedChildren = 0,
        removedChildren = 0,
        metadataChanged = false,
    )

    private fun plugin(vararg bindings: EntryInteractionProviderBinding<*>): EntryInteractionPlugin =
        object : EntryInteractionPlugin {
            override val type = EntryType.BOOK
            override val owner = ContributionOwner("test.type.anonymous")
            override val providerBindings = bindings.toList()
        }

    private fun request(
        source: UnifiedSource? = this.source,
        children: List<EntryChapter> = emptyList(),
    ) = EntryImmersiveLoadRequest(
        context = context,
        entry = entry,
        source = source,
        children = children,
        memberIds = listOf(entry.id),
    )

    private fun catalogueSource(
        optedIn: Boolean,
        declaredTypes: Set<EntryType>? = null,
    ): EntryCatalogueSource {
        val additionalInterfaces = if (declaredTypes == null) emptyArray() else arrayOf(SourceMetadata::class)
        val source = mockk<EntryCatalogueSource>(relaxed = true, moreInterfaces = additionalInterfaces)
        every { source.supportsImmersiveFeed } returns optedIn
        if (declaredTypes != null) {
            every { (source as SourceMetadata).supportedEntryTypes } returns declaredTypes
        }
        return source
    }

    private class RecordingImmersiveProcessor(
        override val loadMode: EntryImmersiveLoadMode = EntryImmersiveLoadMode.ENTRY,
        override val preloadRadius: Int = 2,
        private val failure: Throwable? = null,
        private val rendererFailure: Throwable? = null,
    ) : EntryImmersiveProcessor {
        override val type = EntryType.BOOK
        val renderer = mockk<EntryImmersiveRenderer>()
        var loadedChild: EntryChapter? = null
        var progressCount = 0
        var releaseCount = 0

        override suspend fun load(
            context: Context,
            entry: Entry,
            chapter: EntryChapter?,
            source: UnifiedSource,
        ): EntryImmersiveHandle {
            failure?.let { throw it }
            loadedChild = chapter
            return EntryImmersiveHandle.ImagePages(type, chapter?.id, delegate = Unit)
        }

        override fun renderer(handle: EntryImmersiveHandle): EntryImmersiveRenderer {
            rendererFailure?.let { throw it }
            return renderer
        }

        override suspend fun persistProgress(handle: EntryImmersiveHandle, progress: EntryImmersiveProgress) {
            progressCount++
        }

        override fun release(handle: EntryImmersiveHandle) {
            releaseCount++
        }
    }

    private class ReverseChildListProcessor : EntryChildListProcessor {
        override val type = EntryType.BOOK

        override fun sortedForReading(
            entry: Entry,
            chapters: List<EntryChapter>,
            memberIds: List<Long>,
        ) = chapters.reversed()

        override fun sortedForDisplay(
            entry: Entry,
            chapters: List<EntryChapter>,
            memberIds: List<Long>,
        ) = chapters
    }

    private class RecordingOpenProcessor : EntryOpenProcessor {
        override val type = EntryType.BOOK

        override fun open(
            context: Context,
            entry: Entry,
            chapter: EntryChapter,
            options: EntryOpenOptions,
        ) = Unit

        override fun pendingIntent(
            context: Context,
            entry: Entry,
            chapter: EntryChapter,
            options: EntryOpenOptions,
        ): PendingIntent = mockk()
    }
}

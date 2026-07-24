package mihon.entry.interactions

import android.app.PendingIntent
import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryPreviewSource
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.settings.EntryInteractionPreferences
import mihon.feature.graph.ContributionOwner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

class EntryPreviewFeatureTest {
    private val context = mockk<Context>(relaxed = true)
    private val source = mockk<UnifiedSource>(relaxed = true)
    private val entry = Entry.create().copy(id = 1L, type = EntryType.BOOK)
    private val first = EntryChapter.create().copy(id = 11L, entryId = entry.id)
    private val second = EntryChapter.create().copy(id = 12L, entryId = entry.id)

    @Test
    fun `provider absence is valid and returns structured inapplicability`() = runTest {
        val feature = featureFor()

        feature.isApplicable(entry.type) shouldBe false
        feature.settings.shouldBeEmpty()
        feature.availability(previewContext()).shouldBeInstanceOf<EntryPreviewAvailability.Inapplicable>()
        feature.load(request()).shouldBeInstanceOf<EntryPreviewLoadResult.Inapplicable>()
    }

    @Test
    fun `preview provider activates fixed configuration and lifecycle without settings opt in`() = runTest {
        val processor = RecordingPreviewProcessor()
        val feature = featureFor(EntryPreviewCapability.bind(processor))

        feature.availability(previewContext()) shouldBe EntryPreviewAvailability.Available(EntryPreviewConfig.Default)
        feature.isOpenApplicable(entry.type) shouldBe false
        feature.settings.shouldBeEmpty()
        val handle = feature.load(request()).shouldBeInstanceOf<EntryPreviewLoadResult.Loaded>().handle
        feature.release(handle)

        processor.loadedChild shouldBe null
        processor.releaseCount shouldBe 1
        assertThrows<IllegalStateException> {
            feature.release(handle.copy(entryType = EntryType.MANGA))
        }
    }

    @Test
    fun `independent configuration provider contributes settings and disabled state`() {
        val processor = RecordingPreviewProcessor()
        val configuration = RecordingConfigurationProvider(enabled = false)
        val feature = featureFor(
            EntryPreviewCapability.bind(processor),
            EntryPreviewConfigurationCapability.bind(configuration),
        )

        feature.settings.map(EntryPreviewSettings::type) shouldBe listOf(entry.type)
        feature.availability(previewContext()) shouldBe EntryPreviewAvailability.Disabled(configuration.config())
    }

    @Test
    fun `source requirement is resolved by the Feature instead of the type provider`() {
        val processor = RecordingPreviewProcessor(
            sourceRequirement = EntryPreviewSourceRequirement.PREVIEW_CAPABILITY,
        )
        val feature = featureFor(EntryPreviewCapability.bind(processor))

        feature.availability(previewContext()) shouldBe EntryPreviewAvailability.ContextuallyUnavailable(
            config = EntryPreviewConfig.Default,
            reason = EntryPreviewUnavailableReason.SourceUnsupported,
        )
        val previewSource = mockk<EntryPreviewSource>(relaxed = true)
        feature.availability(EntryPreviewContext(entry, previewSource)) shouldBe
            EntryPreviewAvailability.Available(EntryPreviewConfig.Default)
    }

    @Test
    fun `child backed provider without Child List fails with actionable architecture error`() {
        val error = assertThrows<IllegalStateException> {
            featureFor(
                EntryPreviewCapability.bind(
                    RecordingPreviewProcessor(loadMode = EntryPreviewLoadMode.FIRST_READING_CHILD),
                ),
            )
        }

        error.message shouldBe
            "Child-backed Preview providers require the Preview + Child List relationship; " +
            "missing EntryChildList providers for [BOOK]"
    }

    @Test
    fun `Child List and Open relationships activate selection and open targets automatically`() = runTest {
        val processor = RecordingPreviewProcessor(loadMode = EntryPreviewLoadMode.FIRST_READING_CHILD)
        val feature = featureFor(
            EntryPreviewCapability.bind(processor),
            EntryChildListCapability.bind(ReverseChildListProcessor()),
            EntryOpenCapability.bind(RecordingOpenProcessor()),
        )

        val loaded = feature.load(
            request(
                EntryPreviewChildCandidate(entry, first, source),
                EntryPreviewChildCandidate(entry, second, source),
            ),
        ).shouldBeInstanceOf<EntryPreviewLoadResult.Loaded>()

        processor.loadedChild shouldBe second
        feature.isOpenApplicable(entry.type) shouldBe true
        feature.openTarget(loaded.handle, 0) shouldBe EntryPreviewOpenTargetResult.Available(second.id, 0)
    }

    private fun featureFor(
        vararg bindings: EntryInteractionProviderBinding<*>,
    ): EntryPreviewFeature {
        val composition = createEntryInteractionComposition(
            plugins = listOf(plugin(*bindings)),
            featureContributors = listOf(EntryPreviewFeatureContributor, EntryChildListFeatureContributor),
        )
        val childList = DefaultEntryChildListFeature(
            evaluation = composition.featureGraphEvaluation,
            childList = composition.interactions.childList,
            childProgress = composition.interactions.childProgress,
            missingChildGap = composition.interactions.missingChildGap,
        )
        return DefaultEntryPreviewFeature(
            evaluation = composition.featureGraphEvaluation,
            interaction = composition.interactions.preview,
            childList = childList,
        )
    }

    private fun plugin(vararg bindings: EntryInteractionProviderBinding<*>): EntryInteractionPlugin =
        object : EntryInteractionPlugin {
            override val type = EntryType.BOOK
            override val owner = ContributionOwner("test.type.book")
            override val providerBindings = bindings.toList()
        }

    private fun previewContext() = EntryPreviewContext(entry, source)

    private fun request(vararg children: EntryPreviewChildCandidate) = EntryPreviewLoadRequest(
        context = context,
        previewContext = previewContext(),
        children = children.toList(),
        memberIds = listOf(entry.id),
    )

    private class RecordingPreviewProcessor(
        override val loadMode: EntryPreviewLoadMode = EntryPreviewLoadMode.ENTRY,
        override val sourceRequirement: EntryPreviewSourceRequirement = EntryPreviewSourceRequirement.NONE,
    ) : EntryPreviewProcessor {
        override val type = EntryType.BOOK
        var loadedChild: EntryChapter? = null
        var releaseCount = 0

        override suspend fun loadPreview(
            context: Context,
            entry: Entry,
            chapter: EntryChapter?,
            source: UnifiedSource,
            pageCount: Int,
        ): EntryPreviewHandle {
            loadedChild = chapter
            return EntryPreviewHandle(
                entryType = type,
                chapterId = chapter?.id,
                pages = listOf(
                    EntryPreviewPage(
                        index = 0,
                        status = MutableStateFlow(EntryPreviewPageStatus.Ready),
                        progress = MutableStateFlow(100),
                        imageModel = "preview",
                    ),
                ),
            )
        }

        override suspend fun loadPage(handle: EntryPreviewHandle, pageIndex: Int) = Unit

        override fun release(handle: EntryPreviewHandle) {
            releaseCount++
        }
    }

    private class RecordingConfigurationProvider(enabled: Boolean) : EntryPreviewConfigurationProvider {
        override val type = EntryType.BOOK
        private val preferences = EntryInteractionPreferences(InMemoryPreferenceStore()).apply {
            enableMangaPreview.set(enabled)
        }
        override val settings = EntryPreviewSettings(
            type = type,
            enabled = preferences.enableMangaPreview,
            pageCount = preferences.mangaPreviewPageCount,
            size = preferences.mangaPreviewSize,
        )

        override fun config() = EntryPreviewConfig(
            enabled = settings.enabled.get(),
            pageCount = settings.pageCount.get(),
            size = settings.size.get(),
        )

        override fun configChanges(): Flow<EntryPreviewConfig> = flowOf(config())
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

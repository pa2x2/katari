package mihon.entry.interactions

import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mihon.feature.graph.ContributionOwner
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

class EntryDownloadConfigurationFeatureTest {
    private val entry = Entry.create().copy(id = 7L, source = 11L, type = EntryType.BOOK)
    private val chapter = EntryChapter.create().copy(id = 12L, entryId = entry.id)
    private val context = mockk<Context>()

    @Test
    fun `an options provider activates contextual resolution and selected execution without core download`() = runTest {
        val resolved = EntryDownloadOptions(
            groups = listOf(
                EntryDownloadOptionGroup(
                    key = "quality",
                    label = "Quality",
                    options = listOf(EntryDownloadOption("high", "High")),
                    selectedKey = "high",
                ),
            ),
        )
        val processor = optionsProcessor(resolved)
        val features = featuresFor(EntryDownloadOptionsCapability.bind(processor))

        features.options.isApplicable(EntryType.BOOK) shouldBe true
        features.options.resolve(context, entry, chapter) shouldBe EntryDownloadOptionsResolution.Resolved(resolved)
        val selection = EntryDownloadOptionSelection(mapOf("quality" to "high"))
        features.options.download(entry, listOf(chapter), selection, startNow = true) shouldBe
            EntryDownloadOptionsActionResult.Performed

        coVerify(exactly = 1) { processor.downloadWithOptions(entry, listOf(chapter), selection, true) }
    }

    @Test
    fun `provider absence and contextual option absence remain distinct valid outcomes`() = runTest {
        val unavailable = featuresFor(EntryDownloadOptionsCapability.bind(optionsProcessor(null))).options
        unavailable.resolve(context, entry, chapter) shouldBe EntryDownloadOptionsResolution.ContextuallyUnavailable

        val absent = featuresFor().options
        absent.resolve(context, entry, chapter) shouldBe EntryDownloadOptionsResolution.Inapplicable
        absent.download(entry, listOf(chapter), EntryDownloadOptionSelection(emptyMap())) shouldBe
            EntryDownloadOptionsActionResult.Inapplicable
    }

    @Test
    fun `specialized setting behaviors compose independently without a core downloader`() {
        val provider = settingProvider()
        val features = featuresFor(
            EntryDownloadArchivePackagingCapability.bind(provider),
            EntryDownloadParallelSourceTransfersCapability.bind(provider),
        )

        features.settings.availableSettings shouldBe setOf(
            EntryDownloadSetting.ARCHIVE_PACKAGING,
            EntryDownloadSetting.PARALLEL_SOURCE_TRANSFERS,
        )
    }

    private fun featuresFor(vararg bindings: EntryInteractionProviderBinding<*>): Features {
        val plugin = object : EntryInteractionPlugin {
            override val type = EntryType.BOOK
            override val owner = ContributionOwner("test.partial-download-configuration")
            override val providerBindings = bindings.toList()
        }
        val composition = createEntryInteractionComposition(
            plugins = listOf(plugin),
            featureContributors = listOf(EntryDownloadConfigurationFeatureContributor),
        )
        return Features(
            options = DefaultEntryDownloadOptionsFeature(
                evaluation = composition.featureGraphEvaluation,
                interaction = composition.interactions.download,
            ),
            settings = DefaultEntryDownloadSettingsFeature(composition.featureGraphEvaluation),
        )
    }

    private fun optionsProcessor(options: EntryDownloadOptions?): EntryDownloadOptionsProcessor {
        return mockk(relaxed = true) {
            every { type } returns EntryType.BOOK
            coEvery { resolveDownloadOptions(any(), any(), any()) } returns options
        }
    }

    private fun settingProvider(): EntryDownloadSettingProvider {
        return mockk {
            every { type } returns EntryType.BOOK
        }
    }

    private data class Features(
        val options: EntryDownloadOptionsFeature,
        val settings: EntryDownloadSettingsFeature,
    )
}

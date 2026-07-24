package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.ConfigurableSource
import eu.kanade.tachiyomi.source.entry.EntryPreferenceScreen
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import tachiyomi.domain.source.service.SourceManager

class EntrySourceSettingsFeatureTest {

    @Test
    fun `missing unsupported and supported source ids remain distinct`() {
        val configurable = mockk<ConfigurableSource> {
            every { id } returns 2L
        }
        val unsupported = mockk<UnifiedSource> { every { id } returns 1L }
        val sourceManager = mockk<SourceManager> {
            every { get(0L) } returns null
            every { get(1L) } returns unsupported
            every { get(2L) } returns configurable
            every { getAll() } returns listOf(unsupported, configurable)
        }
        val feature = DefaultEntrySourceSettingsFeature(
            sourceFeatureEvaluation(EntrySourceSettingsFeatureContributor),
            sourceManager,
        )

        feature.resolve(0L) shouldBe EntrySourceSettingsResolution.Missing(0L)
        feature.resolve(1L) shouldBe EntrySourceSettingsResolution.Unsupported(1L)
        feature.supportedSourceIds() shouldBe listOf(2L)
    }

    @Test
    fun `preference population failure is returned to the caller`() {
        val error = IllegalStateException("broken preferences")
        val source = mockk<ConfigurableSource> {
            every { id } returns 2L
            every { getSourcePreferences() } returns mockk()
            every { setupPreferenceScreen(any()) } throws error
        }
        val sourceManager = mockk<SourceManager> { every { get(2L) } returns source }
        val feature = DefaultEntrySourceSettingsFeature(
            sourceFeatureEvaluation(EntrySourceSettingsFeatureContributor),
            sourceManager,
        )

        val result = (feature.resolve(2L) as EntrySourceSettingsResolution.Available)
            .populate(mockk<EntryPreferenceScreen>())

        result shouldBe EntrySourceSettingsPopulateResult.Failed(error)
    }
}

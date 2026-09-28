package mihon.entry.interactions.source

import eu.kanade.tachiyomi.source.entry.ConfigurableSource
import eu.kanade.tachiyomi.source.entry.EntryPreferenceScreen
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import tachiyomi.domain.source.service.SourceManager

class EntrySourceSettingsFeatureTest {

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

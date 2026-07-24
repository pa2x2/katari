package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.UnifiedSource
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test

class EntryTrackerSourceAdapterFeatureTest {

    @Test
    fun `tracker adapter reports unavailable image client`() {
        val feature = DefaultEntryTrackerSourceAdapterFeature(
            evaluation = sourceFeatureEvaluation(EntryTrackerSourceAdapterFeatureContributor),
            sourceManager = mockk { every { get(5L) } returns mockk<UnifiedSource>() },
            settings = mockk {
                every { resolve(5L) } returns EntrySourceSettingsResolution.Available(
                    sourceId = 5L,
                    preferences = mockk(),
                    populateScreen = {},
                )
            },
            home = mockk {
                every { resolve(5L) } returns
                    EntrySourceHomeResolution.Available(5L, "Source", "https://example.test")
            },
        )

        feature.resolve(5L) shouldBe EntryTrackerSourceAdapterResolution.Unavailable(
            sourceId = 5L,
            reasons = setOf(EntryTrackerSourceAdapterUnavailableReason.IMAGE_CLIENT),
        )
    }
}

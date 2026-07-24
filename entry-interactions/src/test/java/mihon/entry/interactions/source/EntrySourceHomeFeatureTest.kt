package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.SourceHomePage
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import tachiyomi.domain.source.service.SourceManager

class EntrySourceHomeFeatureTest {

    @Test
    fun `home resolution distinguishes unavailable outcomes`() {
        val unsupported = mockk<UnifiedSource> { every { id } returns 1L }
        val withoutUrl = homeSource(2L, null)
        val error = IllegalStateException("URL failure")
        val failed = mockk<SourceHomePage> {
            every { id } returns 4L
            every { getHomeUrl() } throws error
        }
        val sourceManager = mockk<SourceManager> {
            every { get(0L) } returns null
            every { get(1L) } returns unsupported
            every { get(2L) } returns withoutUrl
            every { get(4L) } returns failed
        }
        val feature = DefaultEntrySourceHomeFeature(
            sourceFeatureEvaluation(EntrySourceHomeFeatureContributor),
            sourceManager,
        )

        feature.resolve(0L) shouldBe EntrySourceHomeResolution.Missing(0L)
        feature.resolve(1L) shouldBe EntrySourceHomeResolution.Unsupported(1L)
        feature.resolve(2L) shouldBe EntrySourceHomeResolution.NoUrl(2L)
        feature.resolve(4L) shouldBe EntrySourceHomeResolution.Failed(4L, error)
    }

    private fun homeSource(id: Long, url: String?): SourceHomePage = mockk {
        every { this@mockk.id } returns id
        every { name } returns "Source $id"
        every { getHomeUrl() } returns url
    }
}

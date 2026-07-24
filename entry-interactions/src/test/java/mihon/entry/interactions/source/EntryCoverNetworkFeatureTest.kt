package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryImageSource
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import tachiyomi.domain.source.service.SourceManager

class EntryCoverNetworkFeatureTest {

    @Test
    fun `cover network resolution distinguishes missing unsupported and failed sources`() {
        val failure = IllegalStateException("client failure")
        val failed = mockk<EntryImageSource> {
            every { client } throws failure
        }
        val unsupported = mockk<UnifiedSource>()
        val feature = feature(
            mockk {
                every { get(0L) } returns null
                every { get(1L) } returns unsupported
                every { get(2L) } returns failed
            },
        )

        feature.resolve(0L) shouldBe EntryCoverNetworkResolution.Missing(0L)
        feature.resolve(1L) shouldBe EntryCoverNetworkResolution.Unsupported(1L)
        feature.resolve(2L) shouldBe EntryCoverNetworkResolution.Failed(2L, failure)
    }

    private fun feature(sourceManager: SourceManager) = DefaultEntryCoverNetworkFeature(
        evaluation = sourceFeatureEvaluation(EntryCoverNetworkFeatureContributor),
        sourceManager = sourceManager,
    )
}

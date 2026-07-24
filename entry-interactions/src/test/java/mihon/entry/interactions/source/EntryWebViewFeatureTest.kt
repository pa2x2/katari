package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.ChapterWebViewSource
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.source.entry.WebViewSource
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.source.service.SourceManager

class EntryWebViewFeatureTest {

    @Test
    fun `entry web view resolves headers independently`() {
        val source = mockk<WebViewSource> {
            every { id } returns 7L
            every { getWebViewHeaders() } returns mapOf("Authorization" to "token")
        }
        val sourceManager = mockk<SourceManager> { every { get(7L) } returns source }
        val feature = DefaultEntryWebViewFeature(
            sourceFeatureEvaluation(
                EntryWebViewFeatureContributor,
                specializedAdapters = childHostAdapters,
            ),
            sourceManager,
        )

        feature.resolveHeaders(7L) shouldBe
            EntryWebViewHeadersResolution.Available(mapOf("Authorization" to "token"))
    }

    @Test
    fun `missing unsupported and failed web view sources remain distinct`() {
        val unsupported = mockk<UnifiedSource>()
        val error = IllegalStateException("URL failure")
        val failed = mockk<WebViewSource> {
            every { id } returns 2L
            every { getContentUrl(any()) } throws error
        }
        val sourceManager = mockk<SourceManager> {
            every { get(0L) } returns null
            every { get(1L) } returns unsupported
            every { get(2L) } returns failed
        }
        val feature = DefaultEntryWebViewFeature(
            sourceFeatureEvaluation(
                EntryWebViewFeatureContributor,
                specializedAdapters = childHostAdapters,
            ),
            sourceManager,
        )

        feature.resolveEntry(Entry.create().copy(source = 0L)) shouldBe EntryWebViewResolution.Missing(0L)
        feature.resolveEntry(Entry.create().copy(source = 1L)) shouldBe EntryWebViewResolution.Unsupported(1L)
        feature.resolveEntry(Entry.create().copy(source = 2L)) shouldBe EntryWebViewResolution.Failed(2L, error)
    }

    @Test
    fun `child web view distinguishes missing unsupported and failed sources`() {
        val unsupported = mockk<WebViewSource>()
        val error = IllegalStateException("child URL failure")
        val failed = mockk<ChapterWebViewSource> {
            every { getChapterUrl(any()) } throws error
        }
        val sourceManager = mockk<SourceManager> {
            every { get(0L) } returns null
            every { get(1L) } returns unsupported
            every { get(2L) } returns failed
        }
        val feature = DefaultEntryWebViewFeature(
            sourceFeatureEvaluation(
                EntryWebViewFeatureContributor,
                specializedAdapters = childHostAdapters,
            ),
            sourceManager,
        )
        val child = EntryChapter.create()

        feature.resolveChild(Entry.create().copy(source = 0L, type = EntryType.BOOK), child) shouldBe
            EntryChildWebViewResolution.Missing(0L)
        feature.resolveChild(Entry.create().copy(source = 1L, type = EntryType.BOOK), child) shouldBe
            EntryChildWebViewResolution.Unsupported(1L)
        feature.resolveChild(Entry.create().copy(source = 2L, type = EntryType.BOOK), child) shouldBe
            EntryChildWebViewResolution.Failed(2L, error)
    }

    private val childHostAdapters = listOf(
        EntryChildWebViewHostContribution.bind(
            object : EntryChildWebViewHostAdapter {
                override val type = EntryType.BOOK
            },
        ),
    )
}

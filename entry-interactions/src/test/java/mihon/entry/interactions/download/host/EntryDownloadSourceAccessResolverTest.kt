package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.UnifiedSource
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import tachiyomi.domain.source.model.UnifiedStubSource
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.source.local.LocalSource

class EntryDownloadSourceAccessResolverTest {
    private val remoteSourceId = 10L
    private val missingSourceId = 11L
    private val stubSourceId = 12L
    private val sourceManager = mockk<SourceManager> {
        every { get(remoteSourceId) } returns mockk<UnifiedSource>()
        every { get(missingSourceId) } returns null
        every { get(stubSourceId) } returns UnifiedStubSource(stubSourceId, "en", "Missing")
    }
    private val resolver = SourceManagerEntryDownloadSourceAccessResolver(sourceManager)

    @Test
    fun `installed remote sources allow downloads`() {
        resolver.resolve(setOf(remoteSourceId)) shouldBe EntryDownloadSourceAccess.REMOTE
    }

    @Test
    fun `missing stub and local sources block downloads`() {
        resolver.resolve(setOf(missingSourceId)) shouldBe EntryDownloadSourceAccess.LOCAL_OR_STUB
        resolver.resolve(setOf(stubSourceId)) shouldBe EntryDownloadSourceAccess.LOCAL_OR_STUB
        resolver.resolve(setOf(LocalSource.ID)) shouldBe EntryDownloadSourceAccess.LOCAL_OR_STUB
    }

    @Test
    fun `one unavailable source blocks a merged source set`() {
        resolver.resolve(setOf(remoteSourceId, missingSourceId)) shouldBe EntryDownloadSourceAccess.LOCAL_OR_STUB
    }
}

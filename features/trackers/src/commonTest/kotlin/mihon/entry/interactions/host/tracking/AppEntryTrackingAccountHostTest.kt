package mihon.entry.interactions.host.tracking

import eu.kanade.tachiyomi.data.track.EnhancedTracker
import eu.kanade.tachiyomi.data.track.EntryTrackingSource
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.adapter.asUnifiedSource
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.SMangaUpdate
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.source.model.SourceDisplayInfo
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.track.model.EntryTrack

class AppEntryTrackingAccountHostTest {

    @Test
    fun `a Tachiyomi source inside its adapter makes the tracker that accepts its class available`() {
        val tracker = SelfHostedTracker(
            acceptedSources = listOf("mihon.entry.interactions.host.tracking.SelfHostedSource"),
        )

        tracker.acceptsInstalledSource(InstalledSources(OtherSource().asUnifiedSource())) shouldBe false
        tracker.acceptsInstalledSource(
            InstalledSources(OtherSource().asUnifiedSource(), SelfHostedSource().asUnifiedSource()),
        ) shouldBe true
    }
}

private class SelfHostedSource : TestSource(id = 1)

private class OtherSource : TestSource(id = 2)

private abstract class TestSource(override val id: Long) : Source {
    override val name: String = "Source $id"
    override val supportsLatest: Boolean = false

    override suspend fun getPopularManga(page: Int) = MangasPage(emptyList(), hasNextPage = false)

    override suspend fun getLatestUpdates(page: Int) = MangasPage(emptyList(), hasNextPage = false)

    override suspend fun getSearchManga(page: Int, query: String, filters: FilterList) =
        MangasPage(emptyList(), hasNextPage = false)

    override suspend fun getMangaUpdate(
        manga: SManga,
        chapters: List<SChapter>,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ) = SMangaUpdate(manga, chapters)

    override suspend fun getPageList(chapter: SChapter): List<Page> = emptyList()
}

private class SelfHostedTracker(private val acceptedSources: List<String>) : EnhancedTracker {
    override fun getAcceptedSources(): List<String> = acceptedSources

    override fun loginNoop() = Unit

    override suspend fun match(entry: Entry): TrackSearch? = null

    override fun isTrackFrom(track: EntryTrack, entry: Entry, source: EntryTrackingSource?): Boolean = false

    override fun migrateTrack(track: EntryTrack, entry: Entry, newSource: EntryTrackingSource): EntryTrack? = null
}

/** The installed sources, each as the extension loader made it. */
private class InstalledSources(vararg sources: UnifiedSource) : SourceManager {
    private val installed = sources.toList()

    override val isInitialized: StateFlow<Boolean> = MutableStateFlow(true)
    override val sources = flowOf(installed)

    override fun get(sourceKey: Long): UnifiedSource? = installed.find { it.id == sourceKey }

    override fun getOrStub(sourceKey: Long): UnifiedSource = checkNotNull(get(sourceKey))

    override fun getAll(): List<UnifiedSource> = installed

    override fun getStubSources(): List<UnifiedSource> = emptyList()

    override fun getDisplayInfo(sourceKey: Long): SourceDisplayInfo {
        val source = checkNotNull(get(sourceKey))
        return SourceDisplayInfo(source.id, source.name, lang = "", isMissing = false, hasKnownName = true)
    }
}

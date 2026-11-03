package eu.kanade.tachiyomi.source.lnreader.entry

import android.content.SharedPreferences
import eu.kanade.tachiyomi.source.entry.ChapterListRest
import eu.kanade.tachiyomi.source.entry.EntryPreferenceProvider
import eu.kanade.tachiyomi.source.entry.SEntry
import eu.kanade.tachiyomi.source.entry.SEntryChapter
import eu.kanade.tachiyomi.source.lnreader.runtime.LnReaderPluginCalls
import eu.kanade.tachiyomi.source.lnreader.runtime.LnReaderPluginFile
import eu.kanade.tachiyomi.source.lnreader.runtime.LnReaderPlugins
import eu.kanade.tachiyomi.source.lnreader.web.LnReaderUrls
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import mihon.script.runtime.ScriptNetwork
import mihon.script.runtime.ScriptRuntimeFactory
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Test

class LnReaderPagedChaptersTest {

    private val network = ScriptNetwork(OkHttpClient()) { "test" }
    private val plugins = LnReaderPlugins(ScriptRuntimeFactory(network), network, NoPreferences)
    private val novel = SEntry.create().apply {
        url = "/novel"
        title = "Novel"
    }

    // A site that lists its newest chapters on page 1 moves every chapter on by one when a chapter comes out, so an
    // update that reads the last page, as for the usual oldest-first site, finds only the oldest chapter there
    @Test
    fun `a library update of a list whose page 1 is newest finds the chapter added to page 1`() = runBlocking<Unit> {
        val stored = walk(refresh(chapters = 6))

        val update = refresh(chapters = 7).refresh(novel, stored, fetchDetails = false, fetchChapters = true)

        update.chapters.map(SEntryChapter::url) shouldContain "/c/7"
        update.rest shouldBe ChapterListRest.Unchanged
    }

    private suspend fun walk(refresh: LnReaderRefresh): List<SEntryChapter> {
        val first = refresh.refresh(novel, emptyList(), fetchDetails = false, fetchChapters = true)
        val chapters = first.chapters.toMutableList()
        var rest = first.rest
        while (rest is ChapterListRest.More) {
            val part = refresh.olderChapters(novel, rest.token)
            chapters += part.chapters
            rest = part.rest
        }
        rest shouldBe ChapterListRest.Complete
        return chapters
    }

    // Two chapters a page, the newest on page 1
    private fun refresh(chapters: Int): LnReaderRefresh {
        val code = """
            var total = $chapters;
            function page(n) {
              var out = [];
              for (var c = total - (n - 1) * 2; c > total - n * 2 && c > 0; c--) {
                out.push({ name: 'Chapter ' + c, path: '/c/' + c, chapterNumber: c });
              }
              return out;
            }
            exports.default = {
              id: 'paged', name: 'Paged', site: 'https://example.org', version: '1.0.0',
              popularNovels: async function () { return []; },
              searchNovels: async function () { return []; },
              parseChapter: async function () { return '<p>Text</p>'; },
              parseNovel: async function (path) {
                return { path: path, name: 'Novel', chapters: [], totalPages: Math.ceil(total / 2) };
              },
              parsePage: async function (path, n) { return { chapters: page(Number(n)) }; },
            };
        """.trimIndent()
        val calls = LnReaderPluginCalls(LnReaderPluginFile(1L, "paged", code), plugins) { 1L }
        return LnReaderRefresh(
            calls,
            LnReaderUrls("https://example.org", calls, resolvesUrls = false),
            pagedChapters = true,
        )
    }

    private object NoPreferences : EntryPreferenceProvider {
        override fun key(sourceId: Long) = "source_$sourceId"

        override fun preferences(sourceId: Long): SharedPreferences = error("The plugin keeps no settings")
    }
}

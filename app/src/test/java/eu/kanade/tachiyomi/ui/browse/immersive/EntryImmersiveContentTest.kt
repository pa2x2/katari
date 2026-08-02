package eu.kanade.tachiyomi.ui.browse.immersive

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class EntryImmersiveContentTest {

    @Test
    fun `immersive pager key is bundle saveable and includes entry identity`() {
        val mangaKey = entryImmersiveItemKey(EntryImmersiveItemKey(id = 3444L, type = EntryType.MANGA))
        val animeKey = entryImmersiveItemKey(EntryImmersiveItemKey(id = 3444L, type = EntryType.ANIME))

        mangaKey shouldBe "MANGA:3444"
        animeKey shouldBe "ANIME:3444"
    }

    @Test
    fun `pull refresh is only enabled at the settled first page while paging is not blocked`() {
        shouldEnableImmersivePullRefresh(settledPage = 0, pagingBlocked = false) shouldBe true
        shouldEnableImmersivePullRefresh(settledPage = 1, pagingBlocked = false) shouldBe false
        shouldEnableImmersivePullRefresh(settledPage = 0, pagingBlocked = true) shouldBe false
    }
}

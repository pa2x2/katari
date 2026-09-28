package eu.kanade.tachiyomi.ui.browse.immersive

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

class EntryImmersiveContentTest {

    @Test
    fun `immersive pager keys are stable and distinguish entry ids and types`() {
        val mangaKey = entryImmersiveItemKey(EntryImmersiveItemKey(id = 3444L, type = EntryType.MANGA))
        val animeKey = entryImmersiveItemKey(EntryImmersiveItemKey(id = 3444L, type = EntryType.ANIME))

        mangaKey shouldBe entryImmersiveItemKey(EntryImmersiveItemKey(id = 3444L, type = EntryType.MANGA))
        mangaKey shouldNotBe animeKey
        mangaKey shouldNotBe entryImmersiveItemKey(EntryImmersiveItemKey(id = 3445L, type = EntryType.MANGA))
    }
}

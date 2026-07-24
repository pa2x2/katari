package eu.kanade.tachiyomi.data.track

import eu.kanade.tachiyomi.data.track.hikka.Hikka
import eu.kanade.tachiyomi.data.track.mangabaka.MangaBaka
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TrackerIdCompatibilityTest {

    @Test
    fun `new trackers keep stable ids`() {
        val hikka = Hikka(TrackerManager.HIKKA)
        val mangaBaka = MangaBaka(TrackerManager.MANGABAKA)

        hikka.id shouldBe 10L
        mangaBaka.id shouldBe 11L
    }
}

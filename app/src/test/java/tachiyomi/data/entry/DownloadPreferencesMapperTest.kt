package tachiyomi.data.entry

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.VideoDownloadQualityMode

class DownloadPreferencesMapperTest {

    @Test
    fun `quality modes keep their existing stored strings`() {
        val stored = mapOf(
            VideoDownloadQualityMode.BEST to "best",
            VideoDownloadQualityMode.BALANCED to "balanced",
            VideoDownloadQualityMode.DATA_SAVING to "data_saving",
        )

        stored.forEach { (mode, value) ->
            DownloadPreferencesMapper.encodeQualityMode(mode) shouldBe value
            DownloadPreferencesMapper.mapPreferences(0L, 1L, null, null, null, value, 0L).qualityMode shouldBe mode
        }
    }
}

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
            DownloadPreferencesMapper.mapPreferences(
                id = 0L,
                entryId = 1L,
                dubKey = null,
                streamKey = null,
                subtitleKey = null,
                qualityMode = value,
                updatedAt = 0L,
                serverKey = null,
                audioKey = null,
            ).qualityMode shouldBe mode
        }
    }
}

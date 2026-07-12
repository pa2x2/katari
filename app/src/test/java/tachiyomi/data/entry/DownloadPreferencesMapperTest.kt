package tachiyomi.data.entry

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.VideoDownloadQualityMode

class DownloadPreferencesMapperTest {

    @Test
    fun `encodes quality modes with existing stored strings`() {
        DownloadPreferencesMapper.encodeQualityMode(VideoDownloadQualityMode.BEST) shouldBe "best"
        DownloadPreferencesMapper.encodeQualityMode(VideoDownloadQualityMode.BALANCED) shouldBe "balanced"
        DownloadPreferencesMapper.encodeQualityMode(VideoDownloadQualityMode.DATA_SAVING) shouldBe "data_saving"
    }

    @Test
    fun `decodes existing stored quality mode strings`() {
        DownloadPreferencesMapper.mapPreferences(0L, 1L, null, null, null, "best", 0L).qualityMode shouldBe
            VideoDownloadQualityMode.BEST
        DownloadPreferencesMapper.mapPreferences(0L, 1L, null, null, null, "balanced", 0L).qualityMode shouldBe
            VideoDownloadQualityMode.BALANCED
        DownloadPreferencesMapper.mapPreferences(0L, 1L, null, null, null, "data_saving", 0L).qualityMode shouldBe
            VideoDownloadQualityMode.DATA_SAVING
    }
}

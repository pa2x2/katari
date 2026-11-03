package eu.kanade.tachiyomi.ui.stats.recap.cover

import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.toBitmap
import com.materialkolor.hct.Hct
import com.materialkolor.quantize.QuantizerCelebi
import com.materialkolor.score.Score
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.entry.model.EntryCover

/**
 * The most characteristic colour of [cover], as an ARGB int, or null when the cover can't be loaded or has no
 * colour worth building a page around: a greyscale scan, or a faint tint such as grey cloth or aged paper, which a
 * page's palette would have to exaggerate into a colour the cover doesn't have.
 */
suspend fun PlatformContext.recapCoverSeedColor(cover: EntryCover): Int? = withIOContext {
    val request = ImageRequest.Builder(this@recapCoverSeedColor)
        .data(cover)
        .size(SAMPLE_SIZE)
        .readableOnCpu()
        .build()
    val bitmap = SingletonImageLoader.get(this@recapCoverSeedColor).execute(request).image?.toBitmap()
        ?: return@withIOContext null
    Score.score(QuantizerCelebi.quantize(bitmap.argbPixels(), MAX_COLORS), desired = 1, fallbackColorArgb = null)
        .firstOrNull()
        ?.takeIf { Hct.fromInt(it).chroma >= MIN_CHROMA }
}

private const val SAMPLE_SIZE = 96
private const val MAX_COLORS = 128
private const val MIN_CHROMA = 16.0

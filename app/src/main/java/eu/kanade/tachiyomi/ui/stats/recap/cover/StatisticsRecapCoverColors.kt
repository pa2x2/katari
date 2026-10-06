package eu.kanade.tachiyomi.ui.stats.recap.cover

import android.content.Context
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
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
suspend fun Context.recapCoverSeedColor(cover: EntryCover): Int? = withIOContext {
    val request = ImageRequest.Builder(this@recapCoverSeedColor)
        .data(cover)
        .size(SAMPLE_SIZE)
        // Pixels are read back on the CPU, which a hardware bitmap doesn't allow.
        .allowHardware(false)
        .build()
    val bitmap = imageLoader.execute(request).image?.toBitmap() ?: return@withIOContext null
    val pixels = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    Score.score(QuantizerCelebi.quantize(pixels, MAX_COLORS), desired = 1, fallbackColorArgb = null)
        .firstOrNull()
        ?.takeIf { Hct.fromInt(it).chroma >= MIN_CHROMA }
}

private const val SAMPLE_SIZE = 96
private const val MAX_COLORS = 128
private const val MIN_CHROMA = 16.0

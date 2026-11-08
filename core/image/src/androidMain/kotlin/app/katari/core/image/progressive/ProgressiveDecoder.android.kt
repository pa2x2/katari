package app.katari.core.image.progressive

import android.graphics.Bitmap
import tachiyomi.decoder.incremental.IncrementalDecodeOptions
import tachiyomi.decoder.incremental.IncrementalImageDecoder
import tachiyomi.decoder.incremental.newInstance

internal actual fun openProgressiveDecoder(options: ProgressiveImageDecodeOptions): ProgressiveDecoder =
    AndroidProgressiveDecoder(IncrementalImageDecoder.newInstance(options.toIncrementalOptions()))

private class AndroidProgressiveDecoder(private val decoder: IncrementalImageDecoder<Bitmap>) : ProgressiveDecoder {
    override fun append(bytes: ByteArray, offset: Int, length: Int) = decoder.append(bytes, offset, length)

    override fun finish() = decoder.finish()

    override fun pollUpdate(): ProgressiveDecodeUpdate? = decoder.pollUpdate()?.toProgressiveUpdate { it }

    override fun close() = decoder.close()
}

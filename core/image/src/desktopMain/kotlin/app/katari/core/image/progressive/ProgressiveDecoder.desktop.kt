package app.katari.core.image.progressive

import app.katari.core.image.SkiaPixelTarget
import tachiyomi.decoder.incremental.IncrementalImageDecoder
import tachiyomi.decoder.incremental.newInstance

internal actual fun openProgressiveDecoder(options: ProgressiveImageDecodeOptions): ProgressiveDecoder =
    DesktopProgressiveDecoder(
        IncrementalImageDecoder.newInstance(SkiaPixelTarget.allocator, options.toIncrementalOptions()),
    )

private class DesktopProgressiveDecoder(
    private val decoder: IncrementalImageDecoder<SkiaPixelTarget>,
) : ProgressiveDecoder {
    override fun append(bytes: ByteArray, offset: Int, length: Int) = decoder.append(bytes, offset, length)

    override fun finish() = decoder.finish()

    override fun pollUpdate(): ProgressiveDecodeUpdate? = decoder.pollUpdate()?.toProgressiveUpdate(
        SkiaPixelTarget::toPlatformBitmap,
    )

    override fun close() = decoder.close()
}

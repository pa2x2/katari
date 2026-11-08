package app.katari.text.recognition.api.image

/**
 * A host-owned image that recognition reads region by region.
 *
 * Hosts own decoding because only they know every format they display. Coordinates are source-image pixels.
 */
interface TextRecognitionImage {
    /** Stable identity of the encoded image content, used to reuse recognition results. */
    val key: ImageContentKey
    val size: ImageSize

    /**
     * Decodes [region] subsampled by [sampleSize] (a power of two). The returned bitmap is owned by the caller.
     *
     * Each output pixel must average its block of source pixels, as page decoders do: recognition models lose thin
     * strokes that point sampling skips.
     */
    suspend fun decodeRegion(region: ImageRect, sampleSize: Int): TextRecognitionBitmap
}

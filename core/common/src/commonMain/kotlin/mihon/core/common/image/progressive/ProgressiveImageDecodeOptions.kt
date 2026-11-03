package mihon.core.common.image.progressive

class ProgressiveImageDecodeOptions(
    val preferredOutputWidth: Int = DEFAULT_PREFERRED_OUTPUT_WIDTH,
    val maximumBitmapPixels: Long = DEFAULT_MAXIMUM_BITMAP_PIXELS,
    displayProfile: ByteArray? = null,
) {
    internal val displayProfile: ByteArray? = displayProfile?.clone()

    init {
        require(preferredOutputWidth in 1..MAXIMUM_OUTPUT_DIMENSION) {
            "Preferred progressive output width must be between 1 and $MAXIMUM_OUTPUT_DIMENSION"
        }
        require(maximumBitmapPixels in 1..MAXIMUM_BITMAP_PIXELS) {
            "Progressive bitmap pixels must be between 1 and $MAXIMUM_BITMAP_PIXELS"
        }
        require(displayProfile == null || displayProfile.size <= MAXIMUM_DISPLAY_PROFILE_BYTES) {
            "Progressive display profile must not exceed $MAXIMUM_DISPLAY_PROFILE_BYTES bytes"
        }
    }

    companion object {
        const val DEFAULT_PREFERRED_OUTPUT_WIDTH = 2_048
        const val DEFAULT_MAXIMUM_BITMAP_PIXELS = 4_194_304L
        const val MAXIMUM_OUTPUT_DIMENSION = 32_768
        const val MAXIMUM_BITMAP_PIXELS = 67_108_864L
        const val MAXIMUM_DISPLAY_PROFILE_BYTES = 4 * 1_024 * 1_024
    }
}

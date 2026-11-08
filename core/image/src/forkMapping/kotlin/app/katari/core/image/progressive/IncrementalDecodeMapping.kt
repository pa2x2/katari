package app.katari.core.image.progressive

import app.katari.core.image.PlatformBitmap
import tachiyomi.decoder.Format
import tachiyomi.decoder.ImageRegion
import tachiyomi.decoder.incremental.IncrementalBlendOperation
import tachiyomi.decoder.incremental.IncrementalDecodeOptions
import tachiyomi.decoder.incremental.IncrementalDecodeUpdate
import tachiyomi.decoder.incremental.IncrementalDisposalOperation
import tachiyomi.decoder.incremental.IncrementalImageInfo

internal fun ProgressiveImageDecodeOptions.toIncrementalOptions() = IncrementalDecodeOptions(
    preferredOutputWidth = preferredOutputWidth,
    maximumBitmapPixels = maximumBitmapPixels,
    displayProfile = displayProfile,
)

/** This update in the progressive engine's terms, with the platform's pixels turned into a bitmap by [bitmap]. */
internal inline fun <P : Any> IncrementalDecodeUpdate<P>.toProgressiveUpdate(
    bitmap: (P) -> PlatformBitmap,
): ProgressiveDecodeUpdate = when (this) {
    is IncrementalDecodeUpdate.FormatDetected -> ProgressiveDecodeUpdate.FormatDetected(
        format = format.toProgressiveFormat(),
        capabilities = ProgressiveImageCapabilities(
            stillImageUpdates = capabilities.stillImageUpdates,
            animationFrames = capabilities.animationFrames,
        ),
    )
    is IncrementalDecodeUpdate.MetadataAvailable -> ProgressiveDecodeUpdate.MetadataAvailable(
        info.toProgressiveMetadata(),
    )
    is IncrementalDecodeUpdate.StillImageAvailable -> ProgressiveDecodeUpdate.StillImageAvailable(
        pixels = bitmap(pixels),
        updatedRegion = updatedRegion.toProgressiveRegion(),
        generation = generation,
    )
    is IncrementalDecodeUpdate.AnimationFrameAvailable -> ProgressiveDecodeUpdate.AnimationFrameAvailable(
        pixels = bitmap(pixels),
        frame = ProgressiveAnimationFrame(
            index = frame.index,
            durationMillis = frame.durationMillis,
            updatedRegion = frame.updatedRegion.toProgressiveRegion(),
            blendOperation = when (frame.blendOperation) {
                IncrementalBlendOperation.SOURCE -> ProgressiveBlendOperation.SOURCE
                IncrementalBlendOperation.OVER -> ProgressiveBlendOperation.OVER
            },
            disposalOperation = when (frame.disposalOperation) {
                IncrementalDisposalOperation.NONE -> ProgressiveDisposalOperation.NONE
                IncrementalDisposalOperation.BACKGROUND -> ProgressiveDisposalOperation.BACKGROUND
                IncrementalDisposalOperation.PREVIOUS -> ProgressiveDisposalOperation.PREVIOUS
            },
        ),
        generation = generation,
    )
    is IncrementalDecodeUpdate.Complete -> ProgressiveDecodeUpdate.Complete(info.toProgressiveMetadata())
    is IncrementalDecodeUpdate.Unsupported -> ProgressiveDecodeUpdate.Unsupported(format?.toProgressiveFormat())
    is IncrementalDecodeUpdate.Error -> ProgressiveDecodeUpdate.Error(message)
}

internal fun Format.toProgressiveFormat(): ProgressiveImageFormat = when (this) {
    Format.Jpeg -> ProgressiveImageFormat.JPEG
    Format.Png -> ProgressiveImageFormat.PNG
    Format.Webp -> ProgressiveImageFormat.WEBP
    Format.Gif -> ProgressiveImageFormat.GIF
    Format.Heif -> ProgressiveImageFormat.HEIF
    Format.Avif -> ProgressiveImageFormat.AVIF
    Format.Jxl -> ProgressiveImageFormat.JXL
}

private fun IncrementalImageInfo.toProgressiveMetadata() = ProgressiveImageMetadata(
    format = format.toProgressiveFormat(),
    width = width,
    height = height,
    outputWidth = outputWidth,
    outputHeight = outputHeight,
    isAnimated = isAnimated,
    hasAlpha = hasAlpha,
    frameCount = frameCount,
    loopCount = loopCount,
)

private fun ImageRegion.toProgressiveRegion() = ProgressiveImageRegion(left, top, right, bottom)

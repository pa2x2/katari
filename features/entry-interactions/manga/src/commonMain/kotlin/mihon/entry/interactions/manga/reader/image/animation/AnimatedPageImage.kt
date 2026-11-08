package mihon.entry.interactions.manga.reader.image.animation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import mihon.core.image.animation.AnimatedImagePlayback
import mihon.core.image.asImageBitmap
import mihon.core.image.progressive.ProgressiveImageDecodeOptions
import mihon.entry.interactions.manga.reader.image.layout.PageBitmap
import mihon.entry.interactions.manga.reader.image.layout.PageImageLayout
import mihon.entry.interactions.manga.reader.image.source.PageImage

/**
 * Plays an animated page [image] in [layout], stretched over the composable's size, from its first frame on. Calls
 * [onFailed] when the image holds no frame the decoder can show.
 */
@Composable
internal fun AnimatedPageImage(
    image: PageImage.Animated,
    layout: PageImageLayout,
    displayProfile: ByteArray?,
    onFailed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var frame by remember(image) { mutableStateOf<ImageBitmap?>(null) }
    val currentOnFailed by rememberUpdatedState(onFailed)
    LaunchedEffect(image, displayProfile) {
        val playback = AnimatedImagePlayback(
            bytes = image.bytes,
            options = ProgressiveImageDecodeOptions(displayProfile = displayProfile),
        )
        val played = playback.play { frame = it.asImageBitmap() }
        if (!played) currentOnFailed()
    }
    frame?.let { PageBitmap(it, image.size, layout, modifier) }
}

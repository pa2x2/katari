package mihon.entry.viewer.chrome.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import mihon.presentation.kit.surface.MediaBackdrop

@Composable
fun ReaderMediaLoadingBackground(
    previewModel: Any? = null,
    modifier: Modifier = Modifier,
) {
    MediaBackdrop(
        modifier = modifier,
        image = previewModel?.let { model ->
            { imageModifier ->
                AsyncImage(
                    model = model,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = imageModifier,
                )
            }
        },
    )
}

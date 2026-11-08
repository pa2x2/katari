package app.katari.entry.viewer.chrome.ui.loading

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import app.katari.presentation.kit.surface.MediaBackdrop
import coil3.compose.AsyncImage

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

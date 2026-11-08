package eu.kanade.presentation.entry.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import app.katari.presentation.kit.content.ZoomableImage

// The wheel, a double click and dragging zoom and pan, as pinching and double-tapping do on Android.
@Composable
internal actual fun CoverViewerImage(data: Any?, contentPadding: PaddingValues, onClick: () -> Unit) {
    ZoomableImage(model = data, onClick = onClick, contentPadding = contentPadding)
}

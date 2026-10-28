package eu.kanade.presentation.entry.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import tachiyomi.presentation.core.util.clickableNoIndication

// Android zooms through its PhotoView widget, which desktop has no counterpart of; the image is shown whole.
@Composable
internal actual fun CoverViewerImage(data: Any?, contentPadding: PaddingValues, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickableNoIndication(onClick = onClick),
    ) {
        AsyncImage(
            model = data,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        )
    }
}

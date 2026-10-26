package mihon.entry.interactions.book.reader

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import mihon.presentation.kit.feedback.LoadingScreen
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer

@Composable
internal fun BookReaderLoadingScreen(contentDescription: String) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        container = SurfaceContainer.Background,
    ) {
        LoadingScreen(
            modifier = Modifier.semantics {
                this.contentDescription = contentDescription
            },
        )
    }
}

@Composable
internal fun BookReaderDialogBackground() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        container = SurfaceContainer.Background,
    ) {}
}

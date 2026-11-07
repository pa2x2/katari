package mihon.entry.interactions.book.reader

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import mihon.entry.interactions.book.preparation.BookOpeningProgress
import mihon.presentation.kit.feedback.LoadingScreen
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun BookReaderLoadingScreen(progress: BookOpeningProgress?) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        container = SurfaceContainer.Background,
    ) {
        BookOpeningLoadingScreen(progress, stringResource(MR.strings.book_reader_loading))
    }
}

/**
 * The wait while a book opens, naming the step [progress] reports once one has. Until then only the indicator shows,
 * described to accessibility services by [description].
 */
@Composable
internal fun BookOpeningLoadingScreen(
    progress: BookOpeningProgress?,
    description: String,
    modifier: Modifier = Modifier,
) {
    LoadingScreen(
        modifier = if (progress == null) modifier.semantics { contentDescription = description } else modifier,
        message = progress?.let { stringResource(it.message) },
        progress = progress?.barFraction?.let { fraction -> { fraction } },
    )
}

private val BookOpeningProgress.message
    get() = when (this) {
        BookOpeningProgress.Requesting -> MR.strings.book_opening_requesting
        is BookOpeningProgress.Downloading -> MR.strings.book_opening_downloading
        is BookOpeningProgress.Restoring -> MR.strings.book_opening_restoring
        is BookOpeningProgress.Preparing -> MR.strings.book_opening_preparing
        BookOpeningProgress.Saving -> MR.strings.book_opening_saving
    }

private val BookOpeningProgress.barFraction: Float?
    get() = when (this) {
        BookOpeningProgress.Requesting, BookOpeningProgress.Saving -> null
        is BookOpeningProgress.Downloading -> fraction
        is BookOpeningProgress.Restoring -> fraction
        is BookOpeningProgress.Preparing -> fraction
    }

@Composable
internal fun BookReaderDialogBackground() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        container = SurfaceContainer.Background,
    ) {}
}

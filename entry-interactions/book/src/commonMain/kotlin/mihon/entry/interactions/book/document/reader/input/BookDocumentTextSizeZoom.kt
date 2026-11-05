package mihon.entry.interactions.book.document.reader.input

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mihon.entry.interactions.book.document.reader.settings.profileTextSize
import mihon.entry.interactions.book.document.reader.settings.setBookTextSize
import mihon.entry.interactions.reader.settings.BookDocumentReaderSettings
import mihon.entry.viewer.settings.ViewerSettingBinding
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.surface.OverlayPill
import mihon.presentation.kit.surface.OverlayStyle
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The text size changed a step at a time from the keyboard, or with Ctrl and the wheel, for this book as the Text size
 * setting changes it. While steps keep coming only [pending], which [BookDocumentTextSizePill] shows, changes: the size
 * is set, and the pages laid out again, once they stop for a moment.
 */
@Stable
internal class BookDocumentTextSizeZoom(
    private val binding: ViewerSettingBinding<Int>,
    private val scope: CoroutineScope,
) {
    /** The size about to be set, in percent; null once it is set. */
    var pending by mutableStateOf<Int?>(null)
        private set

    private var settle: Job? = null

    fun step(steps: Int) {
        val from = pending ?: binding.state.value.effectiveValue
        show(
            (from + steps * BookDocumentReaderSettings.TEXT_SIZE_STEP_PERCENT)
                .coerceIn(BookDocumentReaderSettings.TEXT_SIZE_RANGE),
        )
    }

    /** Back to the profile's size, which leaves the book without a size of its own. */
    fun reset() = show(binding.profileTextSize)

    private fun show(size: Int) {
        pending = size
        settle?.cancel()
        settle = scope.launch {
            delay(SETTLE_MS)
            binding.setBookTextSize(size)
            pending = null
        }
    }
}

/** Ctrl and the wheel over the pages step [zoom], a step for each notch, rather than scroll or turn a page. */
internal fun Modifier.textSizeWheel(zoom: BookDocumentTextSizeZoom): Modifier = pointerInput(zoom) {
    awaitPointerEventScope {
        var notches = 0f
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (event.type != PointerEventType.Scroll || !event.keyboardModifiers.isCtrlPressed) {
                notches = 0f
                continue
            }
            val change = event.changes.firstOrNull() ?: continue
            change.consume()
            // A touchpad scrolls in fractions of a notch; whole ones make a step, the wheel turned away grows the text
            notches -= change.scrollDelta.y
            val whole = notches.toInt()
            if (whole != 0) {
                notches -= whole
                zoom.step(whole)
            }
        }
    }
}

/** The size [zoom] is about to set, while it is about to set one. */
@Composable
internal fun BookDocumentTextSizePill(zoom: BookDocumentTextSizeZoom, modifier: Modifier = Modifier) {
    val size = zoom.pending ?: return
    OverlayPill(modifier = modifier, style = OverlayStyle.Scrim) {
        Text(
            text = stringResource(MR.strings.book_reader_text_size_value, size),
            role = TextRole.Label,
            modifier = Modifier.padding(horizontal = KatariTheme.spacing.m, vertical = KatariTheme.spacing.s),
        )
    }
}

// Long enough for the next notch or key repeat to arrive, so a run of steps lays the pages out once
private const val SETTLE_MS = 600L

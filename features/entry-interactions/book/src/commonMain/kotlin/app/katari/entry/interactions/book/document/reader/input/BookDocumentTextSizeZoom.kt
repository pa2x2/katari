package app.katari.entry.interactions.book.document.reader.input

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
import app.katari.entry.interactions.book.document.reader.settings.profileTextSize
import app.katari.entry.interactions.book.document.reader.settings.setBookTextSize
import app.katari.entry.interactions.reader.settings.BookDocumentReaderSettings
import app.katari.entry.viewer.settings.ViewerSettingBinding
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.pointer.isFingerOrWheelScroll
import app.katari.presentation.kit.surface.OverlayPill
import app.katari.presentation.kit.surface.OverlayStyle
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The text size changed a step at a time from the keyboard, or with Ctrl and the wheel, for this book as the Text size
 * setting changes it: each step sets the size at once, as the setting's buttons do, and [BookDocumentTextSizePill]
 * shows it for a moment.
 */
@Stable
internal class BookDocumentTextSizeZoom(
    private val binding: ViewerSettingBinding<Int>,
    private val scope: CoroutineScope,
) {
    /** The size last stepped to, in percent, while the pill shows it; null once it hides. */
    var shown by mutableStateOf<Int?>(null)
        private set

    // Saving lags behind the keys, so steps count on from the size last asked for rather than the saved one, and the
    // sizes are saved one after another so the last one asked for is the one kept
    private val requested = Channel<Int>(Channel.CONFLATED)
    private var hide: Job? = null

    init {
        scope.launch {
            for (size in requested) binding.setBookTextSize(size)
        }
    }

    fun step(steps: Int) {
        val from = shown ?: binding.state.value.effectiveValue
        set(
            (from + steps * BookDocumentReaderSettings.TEXT_SIZE_STEP_PERCENT)
                .coerceIn(BookDocumentReaderSettings.TEXT_SIZE_RANGE),
        )
    }

    /** Back to the profile's size, which leaves the book without a size of its own. */
    fun reset() = set(binding.profileTextSize)

    private fun set(size: Int) {
        shown = size
        requested.trySend(size)
        hide?.cancel()
        hide = scope.launch {
            delay(SHOWN_MS)
            shown = null
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
            // The fingers step the size as far as they go, not on with the glide they leave
            if (!event.isFingerOrWheelScroll) continue
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

@Composable
internal fun BookDocumentTextSizePill(zoom: BookDocumentTextSizeZoom, modifier: Modifier = Modifier) {
    val size = zoom.shown ?: return
    OverlayPill(modifier = modifier, style = OverlayStyle.Scrim) {
        Text(
            text = stringResource(MR.strings.book_reader_text_size_value, size),
            role = TextRole.Label,
            modifier = Modifier.padding(horizontal = KatariTheme.spacing.m, vertical = KatariTheme.spacing.s),
        )
    }
}

private const val SHOWN_MS = 1_000L

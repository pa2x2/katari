package mihon.presentation.kit.field

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow

/**
 * A field's text kept in step with a [query] the caller holds: typing is reported through [onQueryChange], and a
 * [query] changed from outside, such as a cleared search, replaces the text.
 *
 * The caller's [query] trails the typing by a frame or more, so a reported text can come back after newer characters
 * were typed. Such an echo is recognised and skipped; writing it into the field would drop those characters.
 */
@Composable
internal fun rememberQueryTextFieldState(query: String, onQueryChange: (String) -> Unit): TextFieldState {
    val state = rememberTextFieldState(query)
    val currentQuery by rememberUpdatedState(query)
    val currentOnQueryChange by rememberUpdatedState(onQueryChange)
    val echoes = remember { QueryEchoes() }
    LaunchedEffect(query) {
        if (!echoes.isEcho(query) && state.text.toString() != query) state.setTextAndPlaceCursorAtEnd(query)
    }
    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }.collect {
            if (it != currentQuery) {
                echoes.reported(it)
                currentOnQueryChange(it)
            }
        }
    }
    return state
}

/** Texts reported to the caller, oldest first, that have not come back as its query yet. */
internal class QueryEchoes {
    private val pending = ArrayDeque<String>()

    fun reported(text: String) {
        pending.addLast(text)
    }

    /**
     * Whether [query] is a reported text coming back. The caller may skip some, so the ones reported before it are
     * settled too. False means the query was changed from outside.
     */
    fun isEcho(query: String): Boolean {
        val index = pending.indexOf(query)
        if (index < 0) {
            pending.clear()
            return false
        }
        repeat(index + 1) { pending.removeFirst() }
        return true
    }
}

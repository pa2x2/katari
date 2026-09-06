package eu.kanade.tachiyomi.ui.browse.source.browse.filter.date

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.layout.Layout
import eu.kanade.tachiyomi.source.entry.filter.EntryDateFilter

internal class DateFilterEditorSession {
    var editing by mutableStateOf<DateFilterEdit?>(null)
        private set

    fun open(filter: EntryDateFilter, onCancel: () -> Unit, onConfirm: (String) -> Unit) {
        editing = DateFilterEdit(filter, onCancel, onConfirm)
    }

    fun cancel() {
        editing?.onCancel?.invoke()
        editing = null
    }
}

internal class DateFilterEdit(
    val filter: EntryDateFilter,
    val onCancel: () -> Unit,
    val onConfirm: (String) -> Unit,
)

internal val LocalDateFilterEditor = staticCompositionLocalOf<DateFilterEditorSession> {
    error("Date filters must be displayed inside a DateFilterEditorHost")
}

/** Keeps the underlying filter composition alive but unplaced while editing a date. */
@Composable
internal fun DateFilterEditorHost(session: DateFilterEditorSession, content: @Composable () -> Unit) {
    val editing = session.editing
    BackHandler(enabled = editing != null, onBack = session::cancel)
    CompositionLocalProvider(LocalDateFilterEditor provides session) {
        Layout(content = {
            Box { content() }
            if (editing != null) {
                PartialDateEditor(editing.filter, onCancel = session::cancel) {
                    editing.onConfirm(it)
                    session.cancel()
                }
            }
        }) { measurables, constraints ->
            val underlying = measurables[0].measure(constraints)
            val foreground = measurables.getOrNull(1)?.measure(constraints) ?: underlying
            layout(foreground.width, foreground.height) { foreground.placeRelative(0, 0) }
        }
    }
}

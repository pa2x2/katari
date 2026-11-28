package eu.kanade.presentation.more.settings.screen.data

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import app.katari.presentation.kit.list.RowGroup
import app.katari.presentation.kit.list.RowGroupGap
import app.katari.presentation.kit.list.RowGroupHeader
import app.katari.presentation.kit.list.RowGroupNote

/** A card of backup or restore options, under an optional section header and over an optional note. */
@Composable
internal fun BackupSection(
    title: String? = null,
    note: String? = null,
    rows: List<@Composable () -> Unit>,
) {
    Column {
        if (title != null) RowGroupHeader(title)
        RowGroup(rows)
        if (note != null) RowGroupNote(note, showIcon = true)
        RowGroupGap()
    }
}

package eu.kanade.presentation.more.settings.screen.data

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import mihon.presentation.kit.list.RowGroup
import mihon.presentation.kit.list.RowGroupGap
import mihon.presentation.kit.list.RowGroupHeader

/** A card of backup or restore options, under an optional section header. */
@Composable
internal fun BackupSection(
    title: String? = null,
    rows: List<@Composable () -> Unit>,
) {
    Column {
        if (title != null) RowGroupHeader(title)
        RowGroup(rows)
        RowGroupGap()
    }
}

package eu.kanade.presentation.library.update.report.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import eu.kanade.presentation.entry.components.EntryCover
import mihon.presentation.kit.content.CoverFrame
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.list.CoverRow
import mihon.presentation.kit.list.SettingsRow
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.TextRole
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.asEntryCover

/**
 * An entry of the report led by its cover, or with a null [entry] a source standing for several entries. [actions]
 * sit under the text, lined up with the title.
 */
@Composable
internal fun ReportRow(
    entry: Entry?,
    title: String,
    subtitle: String?,
    isError: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    actions: (@Composable () -> Unit)? = null,
) {
    val subtitleText: @Composable () -> Unit = {
        if (subtitle != null) {
            Text(
                text = subtitle,
                role = TextRole.Supporting,
                tone = if (isError) ContentTone.Error else ContentTone.Inherit,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
    if (entry == null) {
        SettingsRow(
            title = title,
            trailing = trailing,
            onClick = onClick,
            below = {
                subtitleText()
                actions?.invoke()
            },
        )
        return
    }
    CoverRow(
        cover = {
            EntryCover.Book(
                data = entry.asEntryCover(),
                modifier = Modifier.fillMaxSize(),
                frame = CoverFrame.Thumbnail,
            )
        },
        onClick = onClick,
        trailing = trailing,
    ) {
        Text(text = title, role = TextRole.ItemTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
        subtitleText()
        actions?.invoke()
    }
}

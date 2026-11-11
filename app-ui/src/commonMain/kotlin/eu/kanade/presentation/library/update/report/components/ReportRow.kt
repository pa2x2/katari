package eu.kanade.presentation.library.update.report.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import app.katari.library.update.report.LibraryUpdateReport
import app.katari.presentation.kit.content.CoverFrame
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.content.TonedSpan
import app.katari.presentation.kit.content.TonedText
import app.katari.presentation.kit.list.CoverRow
import app.katari.presentation.kit.list.SettingsRow
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import eu.kanade.presentation.entry.InlineEntryTypeIndicator
import eu.kanade.presentation.entry.components.EntryCover
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.asEntryCover
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * An entry of the report led by its cover, or with a null [entry] a source standing for several entries. [actions]
 * sit under the text, lined up with the title.
 *
 * An entry's details start with its type's icon and end with its [sourceName], as in Updates, so entries of the same
 * title, such as a manga and its novel, tell apart. [onClickCover] opens the entry where the row itself does something
 * else.
 */
@Composable
internal fun ReportRow(
    entry: Entry?,
    title: String,
    subtitle: String?,
    isError: Boolean = false,
    sourceName: String? = null,
    onClick: (() -> Unit)? = null,
    onClickCover: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    actions: (@Composable () -> Unit)? = null,
) {
    val subtitleTone = if (isError) ContentTone.Error else ContentTone.Inherit
    if (entry == null) {
        SettingsRow(
            title = title,
            trailing = trailing,
            onClick = onClick,
            below = {
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        role = TextRole.Supporting,
                        tone = subtitleTone,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                actions?.invoke()
            },
        )
        return
    }
    CoverRow(
        cover = { ReportCover(entry, onClick = onClickCover) },
        onClick = onClick,
        trailing = trailing,
    ) {
        Text(text = title, role = TextRole.ItemTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
        ReportEntryDetails(entry) {
            TonedText(
                spans = listOfNotNull(
                    subtitle?.let { TonedSpan(it, subtitleTone) },
                    sourceName?.let { TonedSpan(it) },
                ),
                role = TextRole.Supporting,
                separator = " · ",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        actions?.invoke()
    }
}

@Composable
internal fun ReportCover(entry: Entry, onClick: (() -> Unit)?) {
    EntryCover.Book(
        data = entry.asEntryCover(),
        modifier = Modifier.fillMaxSize(),
        frame = CoverFrame.Thumbnail,
        onClick = onClick,
    )
}

/** [subtitle] marked as the result of a later recheck when a partial update decided about the entry again. */
@Composable
internal fun LibraryUpdateReport.Item.withRecheck(subtitle: String): String {
    return if (isRechecked) stringResource(MR.strings.library_update_report_with_rechecked, subtitle) else subtitle
}

/** A line of an entry's details led by the icon of its type, as Updates shows them. */
@Composable
internal fun ReportEntryDetails(entry: Entry, content: @Composable () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        entry.type.InlineEntryTypeIndicator()
        content()
    }
}

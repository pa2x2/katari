package mihon.feature.appupdate.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.OpenInNew
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.progress.LinearProgressIndicator
import mihon.presentation.kit.sheet.Sheet
import mihon.presentation.kit.sheet.SheetHeader
import mihon.presentation.kit.surface.Card
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The frame both update sheets share: title, a one-line summary, what the update brings in a card that scrolls inside
 * the capped sheet, then where the download or install stands, then [actions]. Swiping it away is "Not now".
 */
@Composable
internal fun UpdateSheetFrame(
    title: String,
    summary: String,
    detail: UpdateSheetDetail?,
    onOpenReleasePage: (() -> Unit)?,
    onDismiss: () -> Unit,
    actions: @Composable () -> Unit,
    notes: (@Composable ColumnScope.() -> Unit)?,
) {
    Sheet(onDismissRequest = onDismiss, capHeight = true) {
        Column {
            SheetHeader(
                title = title,
                actions = if (onOpenReleasePage != null) {
                    {
                        IconButton(
                            icon = MaterialSymbols.AutoMirroredRounded.OpenInNew,
                            contentDescription = stringResource(MR.strings.update_check_open),
                            onClick = onOpenReleasePage,
                        )
                    }
                } else {
                    null
                },
            )
            Text(
                text = summary,
                modifier = Modifier.padding(horizontal = KatariTheme.spacing.rowInset),
                role = TextRole.Body,
                tone = ContentTone.Secondary,
            )
            if (notes != null) {
                Card(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(
                            start = KatariTheme.spacing.rowInset,
                            end = KatariTheme.spacing.rowInset,
                            top = KatariTheme.spacing.m,
                        )
                        .fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(KatariTheme.spacing.m),
                        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m),
                        content = notes,
                    )
                }
            }
            if (detail != null) {
                Detail(
                    detail = detail,
                    modifier = Modifier.padding(
                        start = KatariTheme.spacing.rowInset,
                        end = KatariTheme.spacing.rowInset,
                        top = KatariTheme.spacing.m,
                    ),
                )
            }
            actions()
        }
    }
}

/** Where a download or install stands, under the notes. */
internal sealed interface UpdateSheetDetail {
    /** [fraction] is 0–1, or null while the size is unknown. */
    data class Progress(val fraction: Float?) : UpdateSheetDetail
    data class Note(val text: String) : UpdateSheetDetail
    data class Error(val text: String) : UpdateSheetDetail
}

@Composable
private fun Detail(detail: UpdateSheetDetail, modifier: Modifier = Modifier) {
    when (detail) {
        is UpdateSheetDetail.Progress -> Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
        ) {
            LinearProgressIndicator(
                progress = detail.fraction?.let { fraction -> { fraction } },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(text = downloadingText(detail.fraction), role = TextRole.Body, tone = ContentTone.Secondary)
        }
        is UpdateSheetDetail.Note -> Text(
            text = detail.text,
            modifier = modifier,
            role = TextRole.Body,
            tone = ContentTone.Secondary,
        )
        is UpdateSheetDetail.Error -> Text(
            text = detail.text,
            modifier = modifier,
            role = TextRole.Body,
            tone = ContentTone.Error,
        )
    }
}

package eu.kanade.presentation.browse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.ArrowForward
import mihon.icons.materialsymbols.roundedfilled.Error
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.list.RowGroupItem
import mihon.presentation.kit.list.RowGroupPosition
import mihon.presentation.kit.list.SettingsRow
import mihon.presentation.kit.progress.LoadingIndicator
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * One source's part of a search, as a card: a row that opens the source, then [content] with what it found. Unlike a
 * list's navigation rows, the row heads its own tappable results, so the arrow tells that it opens the source itself.
 */
@Composable
fun GlobalSearchResultItem(
    title: String,
    subtitle: String?,
    sourceId: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    RowGroupItem(
        position = RowGroupPosition.Alone,
        modifier = modifier.padding(bottom = KatariTheme.spacing.m),
    ) {
        Column {
            SettingsRow(
                title = title,
                subtitle = subtitle,
                leading = { SourceIcon(sourceId = sourceId) },
                trailing = { Icon(MaterialSymbols.AutoMirroredRounded.ArrowForward, contentDescription = null) },
                onClick = onClick,
            )
            content()
        }
    }
}

@Composable
fun GlobalSearchLoadingResultItem() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = KatariTheme.spacing.l),
    ) {
        LoadingIndicator(
            modifier = Modifier
                .size(KatariTheme.sizes.iconSmall)
                .align(Alignment.Center),
        )
    }
}

@Composable
fun GlobalSearchErrorResultItem(message: String?) {
    Column(
        modifier = Modifier
            .padding(horizontal = KatariTheme.spacing.rowInset, vertical = KatariTheme.spacing.s)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs, Alignment.CenterVertically),
    ) {
        Icon(imageVector = MaterialSymbols.RoundedFilled.Error, contentDescription = null)
        Text(
            text = message ?: stringResource(MR.strings.unknown_error),
            role = TextRole.Body,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
internal fun GlobalSearchEmptyResultItem() {
    Text(
        text = stringResource(MR.strings.no_results_found),
        modifier = Modifier.padding(horizontal = KatariTheme.spacing.rowInset, vertical = KatariTheme.spacing.s),
        role = TextRole.Body,
        tone = ContentTone.Secondary,
    )
}

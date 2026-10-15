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
import mihon.icons.materialsymbols.roundedfilled.Error
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.list.SettingsRow
import mihon.presentation.kit.progress.LoadingIndicator
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** One source's part of a search: a row that opens the source, then [content] with what it found. */
@Composable
fun GlobalSearchResultItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier) {
        SettingsRow(title = title, subtitle = subtitle, onClick = onClick)
        content()
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

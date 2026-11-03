package eu.kanade.presentation.library.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun GlobalSearchItem(
    searchQuery: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        label = stringResource(MR.strings.action_global_search_query, searchQuery),
        onClick = onClick,
        modifier = modifier,
        style = ButtonStyle.Text,
    )
}

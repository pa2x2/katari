package eu.kanade.presentation.category.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Add
import app.katari.presentation.kit.button.FloatingActionButton
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.shouldExpandFAB

@Composable
fun CategoryFloatingActionButton(
    lazyListState: LazyListState,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FloatingActionButton(
        label = stringResource(MR.strings.action_add),
        icon = MaterialSymbols.Rounded.Add,
        onClick = onCreate,
        modifier = modifier,
        expanded = lazyListState.shouldExpandFAB(),
    )
}

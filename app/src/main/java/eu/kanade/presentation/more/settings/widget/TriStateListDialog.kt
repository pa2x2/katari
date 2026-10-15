package eu.kanade.presentation.more.settings.widget

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.dialog.Dialog
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.list.TriStateRow
import mihon.presentation.kit.surface.HorizontalDivider
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.core.common.preference.TriState
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun <T> TriStateListDialog(
    title: String,
    message: String? = null,
    items: List<T>,
    initialChecked: List<T>,
    initialInversed: List<T>,
    itemLabel: @Composable (T) -> String,
    onDismissRequest: () -> Unit,
    onValueChanged: (newIncluded: List<T>, newExcluded: List<T>) -> Unit,
) {
    val selected = remember {
        items
            .map {
                when (it) {
                    in initialChecked -> TriState.ENABLED_IS
                    in initialInversed -> TriState.ENABLED_NOT
                    else -> TriState.DISABLED
                }
            }
            .toMutableStateList()
    }
    Dialog(
        onDismissRequest = onDismissRequest,
        title = title,
        confirm = DialogAction(
            label = stringResource(MR.strings.action_ok),
            onClick = {
                val included = items.filterIndexed { index, _ -> selected[index] == TriState.ENABLED_IS }
                val excluded = items.filterIndexed { index, _ -> selected[index] == TriState.ENABLED_NOT }
                onValueChanged(included, excluded)
            },
        ),
        dismiss = DialogAction(stringResource(MR.strings.action_cancel), onClick = onDismissRequest),
    ) {
        Column {
            if (message != null) {
                Text(
                    text = message,
                    modifier = Modifier.padding(bottom = KatariTheme.spacing.s),
                )
            }

            Box {
                val listState = rememberLazyListState()
                LazyColumn(state = listState) {
                    itemsIndexed(items = items) { index, item ->
                        TriStateRow(
                            title = itemLabel(item),
                            state = selected[index],
                            onStateChange = { selected[index] = it },
                        )
                    }
                }

                if (listState.canScrollBackward) HorizontalDivider(modifier = Modifier.align(Alignment.TopCenter))
                if (listState.canScrollForward) HorizontalDivider(modifier = Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}

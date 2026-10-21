package eu.kanade.presentation.entry.components

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.dialog.Dialog
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.dialog.DialogScrollEdges
import mihon.presentation.kit.list.TriStateRow
import tachiyomi.core.common.preference.TriState
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Picks the scanlators whose chapters are hidden; each row toggles between shown and excluded. */
@Composable
fun ScanlatorFilterDialog(
    availableScanlators: Set<String>,
    excludedScanlators: Set<String>,
    onDismissRequest: () -> Unit,
    onConfirm: (Set<String>) -> Unit,
) {
    val sortedAvailableScanlators = remember(availableScanlators) {
        availableScanlators.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it })
    }
    val mutableExcludedScanlators = remember(excludedScanlators) { excludedScanlators.toMutableStateList() }
    val hasScanlators = sortedAvailableScanlators.isNotEmpty()
    Dialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(MR.strings.exclude_scanlators),
        confirm = if (hasScanlators) {
            DialogAction(stringResource(MR.strings.action_ok), onClick = {
                onConfirm(mutableExcludedScanlators.toSet())
                onDismissRequest()
            })
        } else {
            null
        },
        dismiss = DialogAction(stringResource(MR.strings.action_cancel), onDismissRequest),
        neutral = when {
            !hasScanlators -> null
            mutableExcludedScanlators.isEmpty() -> DialogAction(
                stringResource(MR.strings.action_select_all),
                onClick = { mutableExcludedScanlators.addAll(availableScanlators) },
            )
            else -> DialogAction(
                stringResource(MR.strings.action_reset),
                onClick = mutableExcludedScanlators::clear,
            )
        },
    ) {
        if (!hasScanlators) {
            Text(text = stringResource(MR.strings.no_scanlators_found))
            return@Dialog
        }
        val state = rememberLazyListState()
        DialogScrollEdges(state) {
            LazyColumn(state = state) {
                items(sortedAvailableScanlators) { scanlator ->
                    val isExcluded = scanlator in mutableExcludedScanlators
                    TriStateRow(
                        title = scanlator,
                        state = if (isExcluded) TriState.ENABLED_NOT else TriState.DISABLED,
                        onStateChange = {
                            if (isExcluded) {
                                mutableExcludedScanlators.remove(scanlator)
                            } else {
                                mutableExcludedScanlators.add(scanlator)
                            }
                        },
                    )
                }
            }
        }
    }
}

package eu.kanade.presentation.more.settings.widget

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import mihon.presentation.kit.dialog.Dialog
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.dialog.DialogScrollEdges
import mihon.presentation.kit.list.ChoiceRow
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun <T> ListPreferenceWidget(
    value: T,
    title: String,
    subtitle: String?,
    icon: ImageVector?,
    isProfileSpecific: Boolean,
    entries: Map<out T, String>,
    entryEnabled: (T) -> Boolean = { true },
    enabled: Boolean = true,
    onValueChange: (T) -> Unit,
) {
    var isDialogShown by remember { mutableStateOf(false) }

    TextPreferenceWidget(
        title = title,
        subtitle = subtitle,
        icon = icon,
        isProfileSpecific = isProfileSpecific,
        enabled = enabled,
        onPreferenceClick = { isDialogShown = true },
    )

    if (isDialogShown) {
        Dialog(
            onDismissRequest = { isDialogShown = false },
            title = title,
            confirm = DialogAction(stringResource(MR.strings.action_cancel), onClick = { isDialogShown = false }),
        ) {
            val state = rememberLazyListState()
            DialogScrollEdges(state) {
                ScrollbarLazyColumn(state = state) {
                    entries.forEach { current ->
                        val isSelected = value == current.key
                        item {
                            ChoiceRow(
                                title = current.value,
                                selected = isSelected,
                                enabled = current.key?.let(entryEnabled) == true,
                                onClick = {
                                    if (!isSelected) {
                                        onValueChange(current.key!!)
                                        isDialogShown = false
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

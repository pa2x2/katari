package eu.kanade.presentation.more.settings.screen.browse.longpress

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.katari.core.common.CustomPreferences
import app.katari.presentation.kit.list.ReorderHandle
import app.katari.presentation.kit.list.RowGroupNote
import app.katari.presentation.kit.list.SettingsRow
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** One long-press action in a priority list; [handleModifier] makes its drag handle reorder the list. */
@Composable
internal fun LongPressActionRow(
    action: CustomPreferences.BrowseLongPressAction,
    supportingText: String,
    handleModifier: Modifier = Modifier,
    showDragHandle: Boolean = false,
) {
    SettingsRow(
        title = stringResource(action.titleRes),
        subtitle = supportingText,
        trailing = if (showDragHandle) {
            { ReorderHandle(handleModifier) }
        } else {
            null
        },
    )
}

@Composable
internal fun LongPressActionsExplanation() {
    RowGroupNote(stringResource(MR.strings.pref_browse_long_press_action_priority_summary))
}

@Composable
internal fun defaultActionSupportingText(action: CustomPreferences.BrowseLongPressAction): String {
    return when (action) {
        CustomPreferences.BrowseLongPressAction.LIBRARY_ACTION ->
            stringResource(MR.strings.pref_browse_long_press_action_library_available)
        CustomPreferences.BrowseLongPressAction.PREVIEW ->
            stringResource(MR.strings.pref_browse_long_press_action_preview_available)
        CustomPreferences.BrowseLongPressAction.IMMERSIVE ->
            stringResource(MR.strings.pref_browse_long_press_action_immersive_support_info)
    }
}

@Composable
internal fun sourceActionSupportingText(
    action: CustomPreferences.BrowseLongPressAction,
    immersiveAvailable: Boolean,
): String {
    return when (action) {
        CustomPreferences.BrowseLongPressAction.LIBRARY_ACTION ->
            stringResource(MR.strings.pref_browse_long_press_action_library_available)
        CustomPreferences.BrowseLongPressAction.PREVIEW ->
            stringResource(MR.strings.pref_browse_long_press_action_preview_available)
        CustomPreferences.BrowseLongPressAction.IMMERSIVE -> stringResource(
            if (immersiveAvailable) {
                MR.strings.pref_browse_long_press_action_immersive_supported
            } else {
                MR.strings.pref_browse_long_press_action_immersive_unsupported
            },
        )
    }
}

internal fun actionKey(action: CustomPreferences.BrowseLongPressAction): String {
    return "action:${action.name}"
}

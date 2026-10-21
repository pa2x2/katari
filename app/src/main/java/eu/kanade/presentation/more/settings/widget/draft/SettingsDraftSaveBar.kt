package eu.kanade.presentation.more.settings.widget.draft

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.navigation.BottomActionBar
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Shown while a settings screen holds edits that are not stored yet, offering to store or drop them. */
@Composable
fun SettingsDraftSaveBar(
    visible: Boolean,
    saveEnabled: Boolean,
    onDiscard: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier,
    ) {
        BottomActionBar {
            Text(
                text = stringResource(MR.strings.settings_draft_unsaved_changes),
                modifier = Modifier.weight(1f),
                role = TextRole.ItemTitle,
            )
            Button(
                label = stringResource(MR.strings.action_discard),
                onClick = onDiscard,
                style = ButtonStyle.Text,
            )
            Button(
                label = stringResource(MR.strings.action_save),
                onClick = onSave,
                enabled = saveEnabled,
            )
        }
    }
}

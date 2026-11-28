package eu.kanade.presentation.more.settings.screen.data

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Edit
import app.katari.presentation.kit.button.IconButton
import app.katari.presentation.kit.dialog.Dialog
import app.katari.presentation.kit.dialog.DialogAction
import app.katari.presentation.kit.list.ChoiceRow
import app.katari.presentation.kit.list.SettingsRow
import app.katari.presentation.kit.surface.AvatarSize
import app.katari.profiles.core.Profile
import app.katari.profiles.ui.components.ProfileAvatar
import app.katari.profiles.ui.components.ProfileNameDialog
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Which profile a converted backup restores into: the one in use, or one made for it when the restore starts and named
 * after the app it came from (D049-D051).
 */
@Immutable
internal data class RestoreInto(
    val inUse: Profile,
    val intoNewProfile: Boolean,
    /** The name the new profile gets, which no other profile has. */
    val newProfileName: String,
    val newProfileColorSeed: Long,
)

/** The "Restore into" row, which opens [RestoreIntoDialog]. */
@Composable
internal fun RestoreIntoSection(into: RestoreInto, onClick: () -> Unit) {
    BackupSection(
        title = stringResource(MR.strings.restore_into),
        note = stringResource(MR.strings.restore_into_shared_settings).takeIf { into.intoNewProfile },
        rows = listOf {
            SettingsRow(
                title = if (into.intoNewProfile) {
                    stringResource(MR.strings.restore_into_new_profile_named, into.newProfileName)
                } else {
                    into.inUse.name
                },
                subtitle = stringResource(
                    if (into.intoNewProfile) {
                        MR.strings.restore_into_made_on_restore
                    } else {
                        MR.strings.restore_into_profile_in_use
                    },
                ),
                leading = {
                    ProfileAvatar(
                        colorSeed = if (into.intoNewProfile) into.newProfileColorSeed else into.inUse.colorSeed,
                        size = AvatarSize.Row,
                    )
                },
                onClick = onClick,
            )
        },
    )
}

/**
 * Picks the profile in use or a new one; the new one's name is changed through Katari's profile name dialog, which
 * refuses a name [profiles] already holds. Renaming picks the new profile.
 */
@Composable
internal fun RestoreIntoDialog(
    into: RestoreInto,
    profiles: List<Profile>,
    onPick: (intoNewProfile: Boolean) -> Unit,
    onRename: (String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var naming by rememberSaveable { mutableStateOf(false) }
    if (naming) {
        ProfileNameDialog(
            title = stringResource(MR.strings.restore_into_name_title),
            suggestion = into.newProfileName,
            existingProfiles = profiles,
            onDismissRequest = { naming = false },
            onConfirm = {
                onRename(it)
                naming = false
            },
        )
        return
    }
    Dialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(MR.strings.restore_into),
        confirm = null,
        dismiss = DialogAction(stringResource(MR.strings.action_cancel), onDismissRequest),
    ) {
        Column {
            ChoiceRow(
                title = into.inUse.name,
                subtitle = stringResource(MR.strings.restore_into_profile_in_use),
                selected = !into.intoNewProfile,
                avatar = { ProfileAvatar(colorSeed = into.inUse.colorSeed, size = AvatarSize.Row) },
                onClick = { onPick(false) },
            )
            ChoiceRow(
                title = stringResource(MR.strings.restore_into_new_profile),
                subtitle = stringResource(MR.strings.restore_into_named, into.newProfileName),
                selected = into.intoNewProfile,
                avatar = { ProfileAvatar(colorSeed = into.newProfileColorSeed, size = AvatarSize.Row) },
                trailing = {
                    IconButton(
                        icon = MaterialSymbols.Rounded.Edit,
                        contentDescription = stringResource(MR.strings.action_rename),
                        onClick = { naming = true },
                    )
                },
                onClick = { onPick(true) },
            )
        }
    }
}

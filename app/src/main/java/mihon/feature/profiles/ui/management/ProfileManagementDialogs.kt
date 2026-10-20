package mihon.feature.profiles.ui.management

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import mihon.feature.profiles.core.Profile
import mihon.feature.profiles.core.ProfileManager
import mihon.feature.profiles.ui.components.ProfileNameDialog
import mihon.presentation.kit.dialog.ConfirmDialog
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

internal sealed interface ProfileManagementDialog {
    data object Create : ProfileManagementDialog
    data class Rename(val profile: Profile) : ProfileManagementDialog
    data class Archive(val profile: Profile) : ProfileManagementDialog
    data class Delete(val profile: Profile) : ProfileManagementDialog
}

/** Shows [dialog] and carries out what it confirms; [onDismissRequest] closes it, also after a confirmed change. */
@Composable
internal fun ProfileManagementDialogs(
    dialog: ProfileManagementDialog?,
    profiles: List<Profile>,
    profileManager: ProfileManager,
    onDismissRequest: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    when (dialog) {
        null -> Unit
        ProfileManagementDialog.Create -> {
            ProfileNameDialog(
                title = stringResource(MR.strings.profiles_add_profile),
                initialValue = "",
                existingProfiles = profiles,
                onDismissRequest = onDismissRequest,
                onConfirm = { name ->
                    scope.launch {
                        runCatching { profileManager.createProfile(name) }.onSuccess { onDismissRequest() }
                    }
                },
            )
        }
        is ProfileManagementDialog.Rename -> {
            ProfileNameDialog(
                title = stringResource(MR.strings.profiles_rename_profile),
                initialValue = dialog.profile.name,
                existingProfiles = profiles,
                originalProfileId = dialog.profile.id,
                onDismissRequest = onDismissRequest,
                onConfirm = { name ->
                    scope.launch {
                        runCatching { profileManager.renameProfile(dialog.profile.id, name) }
                            .onSuccess { onDismissRequest() }
                    }
                },
            )
        }
        is ProfileManagementDialog.Archive -> {
            ConfirmDialog(
                title = stringResource(MR.strings.profiles_archive_profile),
                text = stringResource(MR.strings.profiles_archive_profile_confirmation, dialog.profile.name),
                confirmLabel = stringResource(MR.strings.action_archive),
                onDismissRequest = onDismissRequest,
                onConfirm = {
                    scope.launch {
                        profileManager.setProfileArchived(dialog.profile.id, archived = true)
                        onDismissRequest()
                    }
                },
            )
        }
        is ProfileManagementDialog.Delete -> {
            ConfirmDialog(
                title = stringResource(MR.strings.profiles_delete_profile),
                text = stringResource(MR.strings.profiles_delete_profile_confirmation, dialog.profile.name),
                confirmLabel = stringResource(MR.strings.action_delete),
                onConfirm = {
                    scope.launch {
                        profileManager.permanentlyDeleteProfile(dialog.profile.id)
                        onDismissRequest()
                    }
                },
                onDismissRequest = onDismissRequest,
                destructive = true,
            )
        }
    }
}

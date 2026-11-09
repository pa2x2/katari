package app.katari.profiles.ui.management

import androidx.compose.runtime.Composable
import app.katari.profiles.core.Profile
import app.katari.sync.client.part.ProfileSync
import app.katari.sync.client.part.ProfileSyncState
import eu.kanade.presentation.more.settings.Preference
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The profiles as settings groups: the visible ones, then the archived ones when there are any. Archived profiles other
 * than the active one can be deleted where [canDelete]. [syncStates] holds, by uuid, whether this device syncs each
 * profile while it is linked, empty while it isn't.
 */
@Composable
internal fun profileListPreferences(
    profiles: List<Profile>,
    activeProfileId: Long?,
    isLocked: (Profile) -> Boolean,
    canDelete: Boolean,
    syncStates: Map<String, ProfileSync>,
    onDialog: (ProfileManagementDialog) -> Unit,
    onRestore: (Profile) -> Unit,
    onSyncChange: (ProfileSync, Boolean) -> Unit,
): List<Preference> {
    val (archived, visible) = profiles.partition(Profile::isArchived)
    fun rows(profiles: List<Profile>) = profiles.map { profile ->
        val sync = syncStates[profile.uuid]?.takeUnless { it.isDefault }
        Preference.PreferenceItem.CustomPreference(title = profile.name) {
            ProfileRow(
                profile = profile,
                isActive = profile.id == activeProfileId,
                isLocked = isLocked(profile),
                syncs = sync?.let { it.state == ProfileSyncState.SYNCED },
                onRename = { onDialog(ProfileManagementDialog.Rename(profile)) },
                onArchive = {
                    if (profile.isArchived) onRestore(profile) else onDialog(ProfileManagementDialog.Archive(profile))
                },
                onSyncChange = { on -> sync?.let { onSyncChange(it, on) } },
                onDelete = if (profile.isArchived && canDelete && profile.id != activeProfileId) {
                    { onDialog(ProfileManagementDialog.Delete(profile)) }
                } else {
                    null
                },
            )
        }
    }
    return listOfNotNull(
        Preference.PreferenceGroup(
            title = stringResource(MR.strings.profiles_visible),
            preferenceItems = rows(visible),
        ),
        archived.takeIf { it.isNotEmpty() }?.let {
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.profiles_archived),
                preferenceItems = rows(it),
            )
        },
    )
}

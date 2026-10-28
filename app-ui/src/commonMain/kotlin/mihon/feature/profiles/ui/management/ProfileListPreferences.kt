package mihon.feature.profiles.ui.management

import androidx.compose.runtime.Composable
import eu.kanade.presentation.more.settings.Preference
import mihon.feature.profiles.core.Profile
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The profiles as settings groups: the visible ones, then the archived ones when there are any. */
@Composable
internal fun profileListPreferences(
    profiles: List<Profile>,
    activeProfileId: Long?,
    isLocked: (Profile) -> Boolean,
    onDialog: (ProfileManagementDialog) -> Unit,
    onRestore: (Profile) -> Unit,
): List<Preference> {
    val (archived, visible) = profiles.partition(Profile::isArchived)
    fun rows(profiles: List<Profile>) = profiles.map { profile ->
        Preference.PreferenceItem.CustomPreference(title = profile.name) {
            ProfileRow(
                profile = profile,
                isActive = profile.id == activeProfileId,
                isLocked = isLocked(profile),
                onRename = { onDialog(ProfileManagementDialog.Rename(profile)) },
                onArchive = {
                    if (profile.isArchived) onRestore(profile) else onDialog(ProfileManagementDialog.Archive(profile))
                },
                onDelete = if (profile.isArchived) {
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

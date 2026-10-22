package mihon.feature.profiles.ui.management

import androidx.compose.runtime.Composable
import mihon.feature.profiles.core.Profile
import mihon.feature.profiles.core.ProfileConstants
import mihon.feature.profiles.ui.components.ProfileAvatar
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Archive
import mihon.icons.materialsymbols.rounded.Delete
import mihon.icons.materialsymbols.rounded.Edit
import mihon.icons.materialsymbols.rounded.Unarchive
import mihon.presentation.kit.content.Badge
import mihon.presentation.kit.content.BadgeGroup
import mihon.presentation.kit.content.BadgeTone
import mihon.presentation.kit.list.SettingsRow
import mihon.presentation.kit.menu.DropdownMenuDivider
import mihon.presentation.kit.menu.DropdownMenuItem
import mihon.presentation.kit.menu.OverflowMenu
import mihon.presentation.kit.surface.AvatarSize
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * A profile in profile management: its avatar, marked active, default, archived or locked, with its actions in a menu.
 * The active and the default profile cannot be archived; only archived profiles can be deleted, through [onDelete].
 */
@Composable
internal fun ProfileRow(
    profile: Profile,
    isActive: Boolean,
    isLocked: Boolean,
    onRename: () -> Unit,
    onArchive: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    val isDefault = profile.id == ProfileConstants.DEFAULT_PROFILE_ID
    SettingsRow(
        title = profile.name,
        subtitle = if (isLocked) stringResource(MR.strings.profiles_locked) else null,
        leading = { ProfileAvatar(colorSeed = profile.colorSeed, size = AvatarSize.Row, isLocked = isLocked) },
        titleSuffix = if (isActive || isDefault || profile.isArchived) {
            {
                BadgeGroup {
                    if (isActive) Badge(text = stringResource(MR.strings.profiles_active), tone = BadgeTone.Primary)
                    if (isDefault) Badge(text = stringResource(MR.strings.profiles_default), tone = BadgeTone.Neutral)
                    if (profile.isArchived) {
                        Badge(text = stringResource(MR.strings.profiles_archived), tone = BadgeTone.Neutral)
                    }
                }
            }
        } else {
            null
        },
        trailing = {
            OverflowMenu { close ->
                if (profile.isArchived) {
                    DropdownMenuItem(
                        label = stringResource(MR.strings.action_restore),
                        onClick = {
                            close()
                            onArchive()
                        },
                        icon = MaterialSymbols.Rounded.Unarchive,
                    )
                }
                DropdownMenuItem(
                    label = stringResource(MR.strings.action_rename),
                    onClick = {
                        close()
                        onRename()
                    },
                    icon = MaterialSymbols.Rounded.Edit,
                )
                if (!profile.isArchived && !isActive && !isDefault) {
                    DropdownMenuItem(
                        label = stringResource(MR.strings.action_archive),
                        onClick = {
                            close()
                            onArchive()
                        },
                        icon = MaterialSymbols.Rounded.Archive,
                    )
                }
                if (onDelete != null) {
                    DropdownMenuDivider()
                    DropdownMenuItem(
                        label = stringResource(MR.strings.action_delete),
                        onClick = {
                            close()
                            onDelete()
                        },
                        icon = MaterialSymbols.Rounded.Delete,
                    )
                }
            }
        },
    )
}

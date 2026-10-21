package mihon.feature.profiles.ui.components

import androidx.compose.runtime.Composable
import mihon.presentation.kit.list.CheckRow
import mihon.presentation.kit.surface.AvatarSize

/** A profile to include in a backup or a restore, with its avatar and a lock badge when it's locked on this device. */
@Composable
internal fun ProfileCheckRow(
    name: String,
    colorSeed: Long,
    isLocked: Boolean,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
) {
    CheckRow(
        title = name,
        checked = checked,
        onCheckedChange = onCheckedChange,
        subtitle = subtitle,
        avatar = { ProfileAvatar(colorSeed = colorSeed, size = AvatarSize.Row, isLocked = isLocked) },
    )
}

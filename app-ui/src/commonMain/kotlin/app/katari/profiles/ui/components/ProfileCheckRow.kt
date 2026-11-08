package app.katari.profiles.ui.components

import androidx.compose.runtime.Composable
import app.katari.presentation.kit.list.CheckRow
import app.katari.presentation.kit.surface.AvatarSize

/** A profile to include in a backup or a restore, with its avatar and a lock badge when it's locked on this device. */
@Composable
fun ProfileCheckRow(
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

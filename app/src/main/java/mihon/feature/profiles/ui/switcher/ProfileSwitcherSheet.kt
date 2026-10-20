package mihon.feature.profiles.ui.switcher

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import mihon.feature.profiles.core.Profile
import mihon.feature.profiles.ui.components.ProfileAvatar
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.CheckCircle
import mihon.icons.materialsymbols.rounded.Settings
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.list.SettingsRow
import mihon.presentation.kit.sheet.Sheet
import mihon.presentation.kit.sheet.SheetHeader
import mihon.presentation.kit.surface.AvatarSize
import mihon.presentation.kit.surface.HorizontalDivider
import mihon.presentation.kit.theme.ContentTone
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The visible profiles to switch between, the active one ticked and locked ones marked, then profile management. */
@Composable
internal fun ProfileSwitcherSheet(
    profiles: List<Profile>,
    activeProfileId: Long,
    isLocked: (Profile) -> Boolean,
    onSelect: (Profile) -> Unit,
    onManageProfiles: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    Sheet(onDismissRequest = onDismissRequest, capHeight = true) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            SheetHeader(title = stringResource(MR.strings.profiles_switch_summary))
            profiles.forEach { profile ->
                val isActive = profile.id == activeProfileId
                val locked = !isActive && isLocked(profile)
                SettingsRow(
                    title = profile.name,
                    subtitle = when {
                        isActive -> stringResource(MR.strings.profiles_active)
                        locked -> stringResource(MR.strings.profiles_locked)
                        else -> null
                    },
                    leading = {
                        ProfileAvatar(colorSeed = profile.colorSeed, size = AvatarSize.Row, isLocked = locked)
                    },
                    trailing = if (isActive) {
                        {
                            Icon(
                                MaterialSymbols.Rounded.CheckCircle,
                                contentDescription = null,
                                tone = ContentTone.Primary,
                            )
                        }
                    } else {
                        null
                    },
                    modifier = Modifier.semantics { selected = isActive },
                    onClick = { onSelect(profile) },
                )
            }
            HorizontalDivider()
            SettingsRow(
                title = stringResource(MR.strings.profiles_manage_title),
                icon = MaterialSymbols.Rounded.Settings,
                onClick = onManageProfiles,
            )
        }
    }
}

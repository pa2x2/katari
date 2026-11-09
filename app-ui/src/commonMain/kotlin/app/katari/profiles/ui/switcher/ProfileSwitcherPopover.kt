package app.katari.profiles.ui.switcher

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.CheckCircle
import app.katari.icons.materialsymbols.rounded.Settings
import app.katari.presentation.kit.content.Icon
import app.katari.presentation.kit.list.RowGroup
import app.katari.presentation.kit.list.RowGroupGap
import app.katari.presentation.kit.list.SettingsRow
import app.katari.presentation.kit.sheet.Sheet
import app.katari.presentation.kit.sheet.SheetHeader
import app.katari.presentation.kit.surface.AvatarSize
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.profiles.core.Profile
import app.katari.profiles.ui.components.ProfileAvatar
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
            RowGroup {
                profiles.forEach { profile ->
                    val isActive = profile.id == activeProfileId
                    row(key = profile.id) {
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
                }
            }
            RowGroupGap()
            RowGroup {
                row {
                    SettingsRow(
                        title = stringResource(MR.strings.profiles_manage_title),
                        icon = MaterialSymbols.Rounded.Settings,
                        onClick = onManageProfiles,
                    )
                }
            }
            RowGroupGap()
        }
    }
}

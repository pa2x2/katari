package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import eu.kanade.presentation.components.GuideButton
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.PreferenceScaffold
import eu.kanade.presentation.util.LocalBackPress
import kotlinx.coroutines.launch
import mihon.app.di.appGraph
import mihon.feature.profiles.ui.management.ProfileManagementDialog
import mihon.feature.profiles.ui.management.ProfileManagementDialogs
import mihon.feature.profiles.ui.management.profileListPreferences
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Add
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.dialog.Dialog
import mihon.presentation.kit.dialog.DialogAction
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The profiles themselves, managed in place, followed by the settings about profiles that search can find. */
object SettingsProfilesScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.profiles_title

    @Composable
    override fun getPreferences(): List<Preference> {
        val context = LocalContext.current
        val profilesPreferences = remember { context.appGraph.profilesPreferences }
        var showProfilesInfo by rememberSaveable { mutableStateOf(false) }

        if (showProfilesInfo) {
            Dialog(
                onDismissRequest = { showProfilesInfo = false },
                title = stringResource(MR.strings.profiles_info_title),
                confirm = DialogAction(stringResource(MR.strings.action_close), onClick = { showProfilesInfo = false }),
            ) {
                Text(stringResource(MR.strings.profiles_info_description))
            }
        }

        return listOf(
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.profiles_startup),
                preferenceItems = listOf(
                    Preference.PreferenceItem.SwitchPreference(
                        preference = profilesPreferences.pickerEnabled,
                        title = stringResource(MR.strings.profiles_choose_on_launch),
                    ),
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.profiles_info_title),
                        onClick = { showProfilesInfo = true },
                    ),
                ),
            ),
        )
    }

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val profileManager = remember { context.appGraph.profileManager }
        val scope = rememberCoroutineScope()
        val profiles by profileManager.profiles.collectAsState()
        val activeProfile by profileManager.activeProfile.collectAsState()
        var dialog by remember { mutableStateOf<ProfileManagementDialog?>(null) }
        val backPress = LocalBackPress.current

        PreferenceScaffold(
            titleRes = getTitleRes(),
            actions = {
                GuideButton(page = "profiles/")
                IconButton(
                    icon = MaterialSymbols.Rounded.Add,
                    contentDescription = stringResource(MR.strings.profiles_add_profile),
                    onClick = { dialog = ProfileManagementDialog.Create },
                )
            },
            onBackPressed = backPress,
            itemsProvider = {
                profileListPreferences(
                    profiles = profiles,
                    activeProfileId = activeProfile?.id,
                    isLocked = { profileManager.profileRequiresUnlock(it.id) },
                    onDialog = { dialog = it },
                    onRestore = { scope.launch { profileManager.setProfileArchived(it.id, archived = false) } },
                ) + getPreferences()
            },
        )
        ProfileManagementDialogs(
            dialog = dialog,
            profiles = profiles,
            profileManager = profileManager,
            onDismissRequest = { dialog = null },
        )
    }
}

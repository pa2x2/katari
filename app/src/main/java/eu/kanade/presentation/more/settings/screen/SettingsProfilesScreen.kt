package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.Preference
import mihon.app.di.appGraph
import mihon.feature.profiles.ui.ProfilesSettingsScreen
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Info
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.dialog.Dialog
import mihon.presentation.kit.dialog.DialogAction
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

object SettingsProfilesScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.profiles_title

    @Composable
    override fun getPreferences(): List<Preference> {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
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
                title = stringResource(MR.strings.profiles_user_profiles),
                preferenceItems = listOf(
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.profiles_manage_title),
                        subtitle = stringResource(MR.strings.profiles_manage_summary),
                        widget = {
                            IconButton(
                                icon = MaterialSymbols.Rounded.Info,
                                contentDescription = stringResource(MR.strings.profiles_info_title),
                                onClick = { showProfilesInfo = true },
                            )
                        },
                        onClick = {
                            navigator.push(ProfilesSettingsScreen())
                        },
                    ),
                    Preference.PreferenceItem.SwitchPreference(
                        preference = profilesPreferences.pickerEnabled,
                        title = stringResource(MR.strings.profiles_choose_on_launch),
                    ),
                ),
            ),
        )
    }
}

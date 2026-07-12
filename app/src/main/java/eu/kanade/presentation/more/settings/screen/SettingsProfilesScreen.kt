package eu.kanade.presentation.more.settings.screen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.Preference
import mihon.feature.profiles.core.ProfilesPreferences
import mihon.feature.profiles.ui.ProfilesSettingsScreen
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object SettingsProfilesScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.profiles_title

    @Composable
    override fun getPreferences(): List<Preference> {
        val navigator = LocalNavigator.currentOrThrow
        val profilesPreferences = remember { Injekt.get<ProfilesPreferences>() }
        var showProfilesInfo by rememberSaveable { mutableStateOf(false) }

        if (showProfilesInfo) {
            AlertDialog(
                onDismissRequest = { showProfilesInfo = false },
                title = { Text(text = stringResource(MR.strings.profiles_info_title)) },
                text = { Text(text = stringResource(MR.strings.profiles_info_description)) },
                confirmButton = {
                    TextButton(onClick = { showProfilesInfo = false }) {
                        Text(text = stringResource(MR.strings.action_close))
                    }
                },
            )
        }

        return listOf(
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.profiles_user_profiles),
                preferenceItems = listOf(
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.profiles_manage_title),
                        subtitle = stringResource(MR.strings.profiles_manage_summary),
                        widget = {
                            IconButton(onClick = { showProfilesInfo = true }) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = stringResource(MR.strings.profiles_info_title),
                                )
                            }
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

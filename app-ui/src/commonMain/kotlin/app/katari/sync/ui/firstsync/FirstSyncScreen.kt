package app.katari.sync.ui.firstsync

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.katari.presentation.kit.button.Button
import app.katari.presentation.kit.button.ButtonStyle
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.feedback.LoadingScreen
import app.katari.presentation.kit.layout.RowList
import app.katari.presentation.kit.list.RowGroupHeader
import app.katari.presentation.kit.list.RowGroupNote
import app.katari.presentation.kit.list.SettingsRow
import app.katari.presentation.kit.list.rowGroupItems
import app.katari.presentation.kit.menu.DropdownMenu
import app.katari.presentation.kit.menu.DropdownMenuItem
import app.katari.presentation.kit.menu.MenuPill
import app.katari.presentation.kit.navigation.AppBar
import app.katari.presentation.kit.navigation.BottomActionBar
import app.katari.presentation.kit.navigation.Scaffold
import app.katari.presentation.kit.surface.AvatarSize
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.TextRole
import app.katari.profiles.core.Profile
import app.katari.profiles.ui.components.ProfileAvatar
import app.katari.sync.client.firstsync.ProfilePairing
import app.katari.sync.client.firstsync.SettingsWinner
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.util.Screen
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The one screen a device with data sees before its first sync (D027, D047): which of its profiles are which of the
 * account's, and whose settings each paired profile keeps. Data is always merged and device-level settings are never
 * replaced. Leaving without starting keeps the device waiting here, which the Sync page offers again.
 */
class FirstSyncScreen : Screen() {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model = metroViewModel<FirstSyncViewModel>()
        val state by model.state.collectAsState()
        val profiles by model.profiles.collectAsState()
        val choosing = state as? FirstSyncViewModel.State.Choosing
        LaunchedEffect(state) { if (state == FirstSyncViewModel.State.Started) navigator.pop() }

        Scaffold(
            topBar = {
                AppBar(
                    title = stringResource(MR.strings.sync_first_sync_title),
                    navigateUp = navigator::pop,
                    scrollBehavior = it,
                )
            },
            bottomBar = {
                BottomActionBar {
                    Button(
                        label = stringResource(MR.strings.action_cancel),
                        onClick = navigator::pop,
                        modifier = Modifier.weight(1f),
                        style = ButtonStyle.Outlined,
                    )
                    Button(
                        label = stringResource(MR.strings.sync_action_start_sync),
                        onClick = model::start,
                        modifier = Modifier.weight(1f),
                        enabled = choosing != null,
                        loading = state == FirstSyncViewModel.State.Starting,
                    )
                }
            },
        ) { contentPadding ->
            when (val shown = state) {
                FirstSyncViewModel.State.Loading,
                FirstSyncViewModel.State.Starting,
                FirstSyncViewModel.State.Started,
                -> LoadingScreen(Modifier.padding(contentPadding))
                FirstSyncViewModel.State.Unavailable -> RowList(contentPadding = contentPadding) {
                    item {
                        RowGroupNote(stringResource(MR.strings.sync_first_sync_unavailable), tone = ContentTone.Error)
                    }
                }
                is FirstSyncViewModel.State.Choosing -> RowList(contentPadding = contentPadding) {
                    val byId = profiles.associateBy { it.id }
                    item { RowGroupNote(stringResource(MR.strings.sync_first_sync_intro)) }
                    item { RowGroupHeader(stringResource(MR.strings.sync_first_sync_profiles)) }
                    rowGroupItems(shown.pairings, key = { "profile-${it.profileId}" }) { pairing ->
                        ProfileRow(
                            pairing = pairing,
                            profile = byId[pairing.profileId],
                            paired = shown.isPaired(pairing),
                            settings = shown.choices.getValue(pairing.profileId).settings,
                            onPairedChange = { model.setPaired(pairing.profileId, it) },
                            onSettingsChange = { model.setSettings(pairing.profileId, it) },
                        )
                    }
                    item {
                        RowGroupNote(stringResource(MR.strings.sync_first_sync_device_settings), showIcon = true)
                    }
                }
            }
        }
    }
}

/**
 * One of this device's profiles, in one row: how it joins the account, with its library's size to tell profiles of
 * one name apart, and, once paired, whose settings it keeps. A profile that matches an account profile only by name
 * gets a pill to pair it or add it as new in place of saying how it pairs.
 */
@Composable
private fun ProfileRow(
    pairing: ProfilePairing,
    profile: Profile?,
    paired: Boolean,
    settings: SettingsWinner,
    onPairedChange: (Boolean) -> Unit,
    onSettingsChange: (SettingsWinner) -> Unit,
) {
    val accountProfile = pairing.pairedWith
    val size = pairing.librarySize
    val titles = pluralStringResource(MR.plurals.sync_first_sync_titles, size.toInt(), size)
    val choosesPairing = accountProfile != null && !pairing.sameProfile
    SettingsRow(
        title = pairing.name,
        subtitle = if (choosesPairing) {
            titles
        } else {
            val pairs = if (paired) MR.strings.sync_first_sync_paired else MR.strings.sync_first_sync_add_new
            stringResource(MR.strings.sync_first_sync_profile_line, stringResource(pairs), titles)
        },
        leading = profile?.let { { ProfileAvatar(colorSeed = it.colorSeed, size = AvatarSize.Row) } },
        below = accountProfile?.takeIf { choosesPairing }?.let {
            {
                ChoicePill(
                    selected = if (paired) 0 else 1,
                    options = listOf(
                        stringResource(MR.strings.sync_first_sync_same_as, it.name),
                        stringResource(MR.strings.sync_first_sync_add_new),
                    ),
                    onSelect = { index -> onPairedChange(index == 0) },
                )
            }
        },
        trailing = if (paired) {
            {
                ChoicePill(
                    selected = settings.ordinal,
                    options = SettingsWinner.entries.map {
                        stringResource(
                            when (it) {
                                SettingsWinner.ACCOUNT -> MR.strings.sync_first_sync_settings_account
                                SettingsWinner.DEVICE -> MR.strings.sync_first_sync_settings_device
                            },
                        )
                    },
                    onSelect = { onSettingsChange(SettingsWinner.entries[it]) },
                )
            }
        } else {
            null
        },
    )
}

/** A row's choice of one of [options], shown as the chosen one on a pill that opens the rest. */
@Composable
private fun ChoicePill(selected: Int, options: List<String>, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        MenuPill(onClick = { expanded = true }) {
            Text(options[selected], role = TextRole.Label)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    label = option,
                    onClick = {
                        expanded = false
                        onSelect(index)
                    },
                    selected = index == selected,
                )
            }
        }
    }
}

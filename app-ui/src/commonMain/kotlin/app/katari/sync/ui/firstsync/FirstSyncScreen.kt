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
import app.katari.sync.client.firstsync.ProfilePairing
import app.katari.sync.client.firstsync.SettingsWinner
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.util.Screen
import mihon.feature.profiles.core.Profile
import mihon.feature.profiles.ui.components.ProfileAvatar
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.feedback.LoadingScreen
import mihon.presentation.kit.layout.RowList
import mihon.presentation.kit.list.RowGroupGap
import mihon.presentation.kit.list.RowGroupHeader
import mihon.presentation.kit.list.RowGroupNote
import mihon.presentation.kit.list.SettingsRow
import mihon.presentation.kit.list.rowGroupItems
import mihon.presentation.kit.menu.DropdownMenu
import mihon.presentation.kit.menu.DropdownMenuItem
import mihon.presentation.kit.menu.MenuPill
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.BottomActionBar
import mihon.presentation.kit.navigation.Scaffold
import mihon.presentation.kit.surface.AvatarSize
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The one screen a device with data sees before its first sync (D027, D047): which of its profiles are which of the
 * account's, and whose settings each paired profile keeps. Data is always merged and device-level settings are never
 * replaced. Leaving without starting keeps the device waiting here, which the Sync page offers again.
 */
data object FirstSyncScreen : Screen() {
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
                        PairingRow(
                            pairing = pairing,
                            profile = byId[pairing.profileId],
                            isDefault = shown.isDefault(pairing),
                            paired = shown.isPaired(pairing),
                            onPairedChange = { model.setPaired(pairing.profileId, it) },
                        )
                    }
                    val paired = shown.pairings.filter(shown::isPaired)
                    item { RowGroupGap() }
                    item { RowGroupHeader(stringResource(MR.strings.sync_first_sync_settings)) }
                    rowGroupItems(paired, key = { "settings-${it.profileId}" }) { pairing ->
                        SettingsChoiceRow(
                            name = pairing.name,
                            winner = shown.choices.getValue(pairing.profileId).settings,
                            onChange = { model.setSettings(pairing.profileId, it) },
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

@Composable
private fun PairingRow(
    pairing: ProfilePairing,
    profile: Profile?,
    isDefault: Boolean,
    paired: Boolean,
    onPairedChange: (Boolean) -> Unit,
) {
    val accountProfile = pairing.pairedWith
    SettingsRow(
        title = pairing.name,
        subtitle = when {
            isDefault -> stringResource(MR.strings.sync_first_sync_default_paired)
            pairing.sameProfile -> stringResource(MR.strings.sync_first_sync_same_profile)
            accountProfile == null -> stringResource(MR.strings.sync_first_sync_add_new)
            else -> null
        },
        leading = profile?.let { { ProfileAvatar(colorSeed = it.colorSeed, size = AvatarSize.Row) } },
        trailing = if (accountProfile != null && !pairing.sameProfile) {
            {
                ChoicePill(
                    selected = if (paired) 0 else 1,
                    options = listOf(
                        stringResource(MR.strings.sync_first_sync_same_as, accountProfile.name),
                        stringResource(MR.strings.sync_first_sync_add_new),
                    ),
                    onSelect = { onPairedChange(it == 0) },
                )
            }
        } else {
            null
        },
    )
}

@Composable
private fun SettingsChoiceRow(name: String, winner: SettingsWinner, onChange: (SettingsWinner) -> Unit) {
    SettingsRow(
        title = name,
        trailing = {
            ChoicePill(
                selected = winner.ordinal,
                options = SettingsWinner.entries.map {
                    stringResource(
                        when (it) {
                            SettingsWinner.ACCOUNT -> MR.strings.sync_first_sync_use_account
                            SettingsWinner.DEVICE -> MR.strings.sync_first_sync_use_device
                        },
                    )
                },
                onSelect = { onChange(SettingsWinner.entries[it]) },
            )
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

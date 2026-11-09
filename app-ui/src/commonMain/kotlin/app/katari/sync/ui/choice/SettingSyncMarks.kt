package app.katari.sync.ui.choice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.CloudSync
import app.katari.icons.materialsymbols.rounded.Smartphone
import app.katari.presentation.kit.content.InlineIcon
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.TextRole
import app.katari.profiles.core.ProfileStore
import app.katari.sync.client.part.ChoiceSet
import app.katari.sync.client.setting.scope.DeviceBound
import app.katari.sync.client.setting.scope.PreferenceScopes
import app.katari.sync.client.status.SyncStatus
import app.katari.sync.ui.status.rememberSyncStatus
import app.katari.ui.di.LocalAppUiGraph
import tachiyomi.core.common.preference.Preference
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Whether a Settings row's setting goes to this device's account or stays on this device. */
enum class SettingSyncMark { SYNCED, THIS_DEVICE }

/**
 * The marks of Settings rows by the preferences they store: a setting this device syncs ([ChoiceSet.syncsSetting]) is
 * synced, and any other a scope table marks stays here. App state and private keys get none, and so do the options of
 * syncing itself ([DeviceBound.SYNC]), which say how this device syncs rather than whether a setting does.
 */
class SettingSyncMarks internal constructor(private val choices: ChoiceSet, private val profileStore: ProfileStore) {
    fun of(preference: Preference<*>): SettingSyncMark? {
        val key = profileStore.preferenceKey(preference)
        val mark = PreferenceScopes.markOf(key)
        return when {
            mark == null || mark.bound == DeviceBound.SYNC -> null
            choices.syncsSetting(key) -> SettingSyncMark.SYNCED
            else -> SettingSyncMark.THIS_DEVICE
        }
    }
}

/** The marks Settings rows show while this device is linked to an account; null while it isn't. */
@Composable
fun rememberSettingSyncMarks(): SettingSyncMarks? {
    val graph = LocalAppUiGraph.current
    val preferences = graph.syncPreferences
    val choices by remember(preferences) { preferences.choicesChanges() }.collectAsState(preferences.choices())
    if (rememberSyncStatus()?.link !is SyncStatus.Link.Linked) return null
    return remember(choices, graph) { SettingSyncMarks(choices, graph.profileStore) }
}

/** A setting's sync mark after its title: a cloud where it syncs, a phone where it stays on this device. */
@Composable
fun SettingSyncMarkIcon(mark: SettingSyncMark) {
    InlineIcon(
        imageVector = when (mark) {
            SettingSyncMark.SYNCED -> MaterialSymbols.Rounded.CloudSync
            SettingSyncMark.THIS_DEVICE -> MaterialSymbols.Rounded.Smartphone
        },
        contentDescription = stringResource(
            when (mark) {
                SettingSyncMark.SYNCED -> MR.strings.sync_mark_synced
                SettingSyncMark.THIS_DEVICE -> MR.strings.sync_mark_this_device
            },
        ),
        role = TextRole.ItemTitle,
        tone = ContentTone.Secondary,
    )
}

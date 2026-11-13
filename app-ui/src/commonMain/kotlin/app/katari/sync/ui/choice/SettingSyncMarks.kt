package app.katari.sync.ui.choice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.katari.presentation.kit.list.LocalSettingMarks
import app.katari.presentation.kit.list.RowGroupHeader
import app.katari.presentation.kit.list.SettingMarks
import app.katari.presentation.kit.theme.TextRole
import app.katari.profiles.core.ProfileStore
import app.katari.sync.client.part.ChoiceSet
import app.katari.sync.client.setting.scope.DeviceBound
import app.katari.sync.client.setting.scope.MenuSettings
import app.katari.sync.client.setting.scope.PreferenceScopes
import app.katari.sync.client.status.SyncStatus
import app.katari.sync.ui.status.rememberSyncStatus
import app.katari.ui.di.LocalAppUiGraph
import tachiyomi.core.common.preference.Preference

/** What a sync mark stands for, by its [title], and whether this device syncs it, given its [choices]. */
sealed interface SyncMark {
    /** Tells the marks on one screen apart, for the state each keeps. */
    val id: String
    val title: String
    val choices: ChoiceSet
    val synced: Boolean get() = syncedBy(choices)

    /** Whether a device with [choices], this one or another of the account, syncs what the mark stands for. */
    fun syncedBy(choices: ChoiceSet): Boolean
}

/** A row's setting: [key] as preference classes ask a store for it, and the row's title. */
class SettingSyncMark(val key: String, override val title: String, override val choices: ChoiceSet) : SyncMark {
    override val id: String get() = key

    override fun syncedBy(choices: ChoiceSet) = choices.syncsSetting(key)
}

/** A menu's settings, which sync or stay as one, such as the library sort. */
class MenuSyncMark(val menu: MenuSettings, override val title: String, override val choices: ChoiceSet) : SyncMark {
    override val id: String get() = menu.name

    override fun syncedBy(choices: ChoiceSet) = choices.syncs(PreferenceScopes.areaOf(menu)) && choices.syncs(menu)
}

/**
 * The marks of Settings rows by the preferences they store: a setting this device syncs ([ChoiceSet.syncsSetting]) is
 * synced, and any other a scope table marks stays here. App state and private keys get none, and so do the options of
 * syncing itself ([DeviceBound.SYNC]), which say how this device syncs rather than whether a setting does.
 */
class SettingSyncMarks internal constructor(private val choices: ChoiceSet, private val profileStore: ProfileStore) {
    fun of(preference: Preference<*>, title: String): SettingSyncMark? =
        markedKey(preference)?.let { SettingSyncMark(it, title, choices) }

    fun has(preference: Preference<*>): Boolean = markedKey(preference) != null

    private fun markedKey(preference: Preference<*>): String? {
        val key = profileStore.preferenceKey(preference)
        val mark = PreferenceScopes.markOf(key)
        return key.takeUnless { mark == null || mark.bound == DeviceBound.SYNC }
    }

    fun of(menu: MenuSettings, title: String): MenuSyncMark =
        MenuSyncMark(menu, title, choices)
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

/**
 * Gives [content] the sync mark of every row that edits a setting ([LocalSettingMarks]), in Settings and in the
 * readers' and players' sheets alike, while this device is linked.
 */
@Composable
internal fun ProvideSettingSyncMarks(content: @Composable () -> Unit) {
    val marks = rememberSettingSyncMarks()
    val settingMarks = remember(marks) { marks?.let(::SyncSettingMarks) }
    CompositionLocalProvider(LocalSettingMarks provides settingMarks, content = content)
}

private class SyncSettingMarks(private val marks: SettingSyncMarks) : SettingMarks {
    override fun hasMark(setting: Preference<*>): Boolean = marks.has(setting)

    @Composable
    override fun Mark(
        setting: Preference<*>,
        title: String,
        role: TextRole,
        open: Boolean,
        onOpenChange: (Boolean) -> Unit,
    ) {
        marks.of(setting, title)?.let { SettingSyncMarkButton(it, role, open, onOpenChange) }
    }
}

/**
 * The header naming [menu]'s settings above the rows that set them, such as the library menu's Sort tab, with the mark
 * that chooses whether they sync; nothing while this device isn't linked, where the menu needs no header.
 */
@Composable
internal fun MenuSyncHeader(menu: MenuSettings) {
    val marks = rememberSettingSyncMarks() ?: return
    val mark = marks.of(menu, menu.title())
    RowGroupHeader(text = mark.title, trailing = { SettingSyncMarkButton(mark, TextRole.GroupLabel) })
}

/**
 * [menu]'s mark alone, after a group heading that names part of the menu, such as the extension filter's languages;
 * nothing while this device isn't linked.
 */
@Composable
internal fun MenuSyncMark(menu: MenuSettings) {
    val marks = rememberSettingSyncMarks() ?: return
    SettingSyncMarkButton(marks.of(menu, menu.title()), TextRole.GroupLabel)
}

package app.katari.sync.ui.choice

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.CloudOff
import app.katari.icons.materialsymbols.rounded.CloudSync
import app.katari.icons.materialsymbols.rounded.Lock
import app.katari.platform.presentation.platformPresentation
import app.katari.presentation.kit.button.InlineIconButton
import app.katari.presentation.kit.dialog.DialogAction
import app.katari.presentation.kit.list.ChoiceRow
import app.katari.presentation.kit.list.InfoRow
import app.katari.presentation.kit.list.RowGroup
import app.katari.presentation.kit.list.RowGroupGap
import app.katari.presentation.kit.list.RowGroupScope
import app.katari.presentation.kit.list.SettingsRow
import app.katari.presentation.kit.list.ToggleRow
import app.katari.presentation.kit.sheet.Popover
import app.katari.presentation.kit.sheet.PopoverAnchor
import app.katari.presentation.kit.sheet.SheetActions
import app.katari.presentation.kit.sheet.SheetHeader
import app.katari.presentation.kit.sheet.popoverAnchor
import app.katari.presentation.kit.sheet.rememberPopoverAnchor
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.TextRole
import app.katari.sync.client.firstsync.SettingsWinner
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.tachiyomi.ui.setting.LocalSettingsNavigator
import eu.kanade.tachiyomi.ui.setting.SettingsScreen
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * A setting's or a menu's sync mark after its title, sized to the title's [role], which opens its sync choice while
 * [open]: a cloud where it syncs, this device where it stays on it. The row around it keeps [open], so a right-click on
 * the row opens the same choice.
 */
@Composable
internal fun SettingSyncMarkButton(mark: SyncMark, role: TextRole, open: Boolean, onOpenChange: (Boolean) -> Unit) {
    val anchor = rememberPopoverAnchor()
    InlineIconButton(
        imageVector = if (mark.synced) MaterialSymbols.Rounded.CloudSync else platformPresentation.thisDeviceIcon,
        contentDescription = stringResource(
            if (mark.synced) MR.strings.sync_mark_synced else MR.strings.sync_mark_this_device,
        ),
        role = role,
        tone = ContentTone.Secondary,
        onClick = { onOpenChange(true) },
        modifier = Modifier.popoverAnchor(anchor),
    )
    if (open) SettingSyncPopover(mark, anchor, onDismissRequest = { onOpenChange(false) })
}

/** [mark] with the sync choice it opens, where nothing around it opens that choice as well, such as a menu's header. */
@Composable
internal fun SettingSyncMarkButton(mark: SyncMark, role: TextRole) {
    var open by rememberSaveable(mark.id) { mutableStateOf(false) }
    SettingSyncMarkButton(mark, role, open) { open = it }
}

/**
 * Whether [mark]'s setting syncs, with what can change it here: its own switch, its menu's or its area's, or why it
 * stays on this device; then, where screens open, the way to the page listing the rest. Turning settings back on that this device changed meanwhile asks in place whose values they keep.
 */
@Composable
private fun SettingSyncPopover(mark: SyncMark, anchor: PopoverAnchor, onDismissRequest: () -> Unit) {
    val model = metroViewModel<WhatSyncsViewModel>()
    val choices by model.choices.collectAsState()
    val asking by model.asking.collectAsState()
    // Whoever opens a mark's choice knows what the mark is
    LaunchedEffect(Unit) { model.preferences.markHintSeen.set(true) }
    val settingsNavigator = LocalSettingsNavigator.current
    // A reader or player in an activity of its own has no screens to open; there the popover keeps to the choice
    val navigator = LocalNavigator.current
    val state = when (mark) {
        is SettingSyncMark -> settingSyncState(mark.key, choices)
        is MenuSyncMark -> menuSyncState(mark.menu, choices)
    } ?: return
    val close = {
        model.dismissQuestion()
        onDismissRequest()
    }
    val open: ((Screen) -> Unit)? = when {
        settingsNavigator != null -> { screen ->
            close()
            settingsNavigator.push(screen)
        }
        navigator != null -> { screen ->
            close()
            navigator.push(SettingsScreen(screen))
        }
        else -> null
    }
    Popover(anchor = anchor, onDismissRequest = close) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            SheetHeader(title = mark.title)
            // A question that came back after another popover closed is about something else
            val again = asking?.takeIf { it.isAbout(state) }
            if (again != null) {
                AgainQuestion(again, onAnswer = model::answer, onCancel = model::dismissQuestion)
            } else {
                RowGroup { stateRows(state, mark, model) }
                RowGroupGap()
                if (open != null) {
                    RowGroup { row { PageLink(state, open) } }
                    RowGroupGap()
                }
            }
        }
    }
}

private fun RowGroupScope.stateRows(state: SettingSyncState, mark: SyncMark, model: WhatSyncsViewModel) {
    when (state) {
        is SettingSyncState.Switch -> row {
            ToggleRow(
                title = stringResource(MR.strings.sync_setting_switch),
                subtitle = state.summary(),
                checked = state.on,
                onCheckedChange = { model.syncSetting(state.key, mark.title, it) },
            )
        }
        is SettingSyncState.Menu -> {
            row {
                ToggleRow(
                    title = stringResource(MR.strings.sync_menu_switch, state.menu.title()),
                    subtitle = state.summary(),
                    checked = state.on,
                    onCheckedChange = { model.syncMenu(state.menu, it) },
                )
            }
            row { InfoRow(stringResource(MR.strings.sync_setting_menu_note), showIcon = false) }
        }
        is SettingSyncState.AreaOff -> {
            row {
                SettingsRow(
                    title = state.summary(),
                    subtitle = stringResource(MR.strings.sync_setting_area_off_summary),
                    icon = MaterialSymbols.Rounded.CloudOff,
                )
            }
            row {
                SettingsRow(
                    title = stringResource(MR.strings.sync_area_switch, state.area.title()),
                    icon = MaterialSymbols.Rounded.CloudSync,
                    onClick = { model.syncArea(state.area, on = true) },
                )
            }
        }
        is SettingSyncState.WithData -> row {
            SettingsRow(
                title = state.summary(),
                subtitle = stringResource(MR.strings.sync_setting_with_data_summary),
                icon = state.group.icon,
            )
        }
        is SettingSyncState.DeviceOnly -> row {
            SettingsRow(
                title = stringResource(MR.strings.sync_mark_this_device),
                subtitle = state.summary(),
                icon = MaterialSymbols.Rounded.Lock,
            )
        }
    }
}

private fun SettingsAgain.isAbout(state: SettingSyncState): Boolean = when (this) {
    is SettingsAgain.Setting -> state is SettingSyncState.Switch && state.key == key
    is SettingsAgain.Area -> state is SettingSyncState.AreaOff && state.area == area
    is SettingsAgain.Menu -> state is SettingSyncState.Menu && state.menu == menu
}

/** The page listing the rest of the setting's area, where it has one, or else What syncs. */
@Composable
private fun PageLink(state: SettingSyncState, open: (Screen) -> Unit) {
    val area = when (state) {
        is SettingSyncState.Switch -> state.area
        is SettingSyncState.Menu -> state.area
        is SettingSyncState.AreaOff -> state.area
        is SettingSyncState.DeviceOnly -> state.area
        is SettingSyncState.WithData -> null
    }?.takeIf { it.hasPage }
    if (area != null) {
        SettingsRow(
            title = stringResource(MR.strings.sync_setting_area_choices, area.title()),
            onClick = { open(SettingsAreaSyncScreen(area)) },
        )
    } else {
        SettingsRow(title = stringResource(MR.strings.sync_choices_title), onClick = { open(WhatSyncsScreen()) })
    }
}

/** [again]'s question in the popover, where the switch was. */
@Composable
private fun AgainQuestion(again: SettingsAgain, onAnswer: (SettingsWinner) -> Unit, onCancel: () -> Unit) {
    var keep by rememberSaveable { mutableStateOf(SettingsWinner.ACCOUNT) }
    RowGroup {
        row { SettingsRow(title = again.questionTitle(), subtitle = again.questionText()) }
        SettingsWinner.entries.forEach { winner ->
            row(key = winner) {
                ChoiceRow(title = again.answer(winner), selected = keep == winner, onClick = { keep = winner })
            }
        }
    }
    SheetActions(
        confirm = DialogAction(stringResource(MR.strings.sync_action_sync), { onAnswer(keep) }),
        dismiss = DialogAction(stringResource(MR.strings.action_cancel), onCancel),
    )
}

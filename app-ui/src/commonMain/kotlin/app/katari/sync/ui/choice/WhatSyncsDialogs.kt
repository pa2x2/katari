package app.katari.sync.ui.choice

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import app.katari.core.platform.currentPlatformContext
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.dialog.ConfirmDialog
import app.katari.presentation.kit.dialog.Dialog
import app.katari.presentation.kit.dialog.DialogAction
import app.katari.presentation.kit.list.ChoiceRow
import app.katari.profiles.core.Profile
import app.katari.profiles.core.ProfileConstants
import app.katari.profiles.ui.switcher.switchToProfile
import app.katari.sync.client.firstsync.SettingsWinner
import app.katari.sync.client.part.ProfileSync
import app.katari.ui.di.LocalAppUiGraph
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Asks whose values the settings [again] keep as they sync again, after this device changed them while they didn't. */
@Composable
internal fun SettingsAgainDialog(
    again: SettingsAgain,
    onAnswer: (SettingsWinner) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var keep by rememberSaveable { mutableStateOf(SettingsWinner.ACCOUNT) }
    Dialog(
        onDismissRequest = onDismissRequest,
        title = again.questionTitle(),
        confirm = DialogAction(stringResource(MR.strings.sync_action_sync), { onAnswer(keep) }),
        dismiss = DialogAction(stringResource(MR.strings.action_cancel), onDismissRequest),
    ) {
        Column {
            Text(again.questionText())
            SettingsWinner.entries.forEach { winner ->
                ChoiceRow(title = again.answer(winner), selected = keep == winner, onClick = { keep = winner })
            }
        }
    }
}

/**
 * Confirms that [profile] leaves this device, staying in the account; where it is the open profile, Katari moves to
 * [switchTo] first.
 */
@Composable
internal fun RemoveProfileDialog(
    profile: ProfileSync,
    switchTo: Profile?,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val text = stringResource(MR.strings.sync_profile_remove_text, profile.name)
    ConfirmDialog(
        title = stringResource(MR.strings.sync_profile_remove_title, profile.name),
        text = switchTo?.let { "$text ${stringResource(MR.strings.sync_profile_remove_open, it.name)}" } ?: text,
        confirmLabel = stringResource(MR.strings.action_remove),
        destructive = true,
        onConfirm = onConfirm,
        onDismissRequest = onDismissRequest,
    )
}

/** Asks whose values settings turned back on keep, while the model waits for the answer. */
@Composable
internal fun SettingsAgainQuestion(model: WhatSyncsViewModel) {
    val again by model.asking.collectAsState()
    again?.let {
        SettingsAgainDialog(again = it, onAnswer = model::answer, onDismissRequest = model::dismissQuestion)
    }
}

/**
 * Confirms that a profile switched off leaves this device. Where it is the open profile, Katari switches to the default
 * profile, which every device syncs, before it goes.
 */
@Composable
internal fun ProfileRemovalQuestion(model: WhatSyncsViewModel) {
    val profile by model.removingProfile.collectAsState()
    val removing = profile ?: return
    val context = currentPlatformContext()
    val graph = LocalAppUiGraph.current
    val profileManager = graph.profileManager
    val active by profileManager.activeProfile.collectAsState()
    val profiles by profileManager.profiles.collectAsState()
    val switchTo = profiles.firstOrNull { it.id == ProfileConstants.DEFAULT_PROFILE_ID }
        ?.takeIf { active?.uuid == removing.uuid }
    RemoveProfileDialog(
        profile = removing,
        switchTo = switchTo,
        onConfirm = {
            model.removeProfile(
                switchAway = switchTo?.let { target ->
                    {
                        switchToProfile(
                            context = context,
                            profileManager = profileManager,
                            profileUnlock = graph.profileUnlock,
                            switchEffects = graph.profileSwitchEffects,
                            profile = target,
                        )
                    }
                },
            )
        },
        onDismissRequest = model::dismissProfileRemoval,
    )
}

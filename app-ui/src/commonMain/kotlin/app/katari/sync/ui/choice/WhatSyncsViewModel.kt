package app.katari.sync.ui.choice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.katari.core.metro.AppCoroutineScope
import app.katari.sync.client.connection.SyncConnection
import app.katari.sync.client.firstsync.SettingsWinner
import app.katari.sync.client.part.ChoiceSet
import app.katari.sync.client.part.DataGroup
import app.katari.sync.client.part.ProfileSync
import app.katari.sync.client.part.ProfileSyncStates
import app.katari.sync.client.part.SyncChoiceChanges
import app.katari.sync.client.preference.SyncPreferences
import app.katari.sync.client.setting.scope.SettingsArea
import app.katari.ui.di.AppUiScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.launchUI

/**
 * What syncs and its area pages: this device's choices as they change, and changing them. A choice made here goes
 * through [SyncChoiceChanges], which saves it and then brings what this device holds in line with it. That can take a
 * while, such as finding a profile's records among all the account's, so it runs in the app's scope: leaving the
 * screen meanwhile would otherwise stop it after the choice was saved, and the device would keep holding back what it
 * now syncs.
 */
@Inject
@ViewModelKey
@ContributesIntoMap(AppUiScope::class)
class WhatSyncsViewModel(
    private val changes: SyncChoiceChanges,
    profileStates: ProfileSyncStates,
    connection: SyncConnection,
    val preferences: SyncPreferences,
    @AppCoroutineScope private val appScope: CoroutineScope,
) : ViewModel() {
    val choices: StateFlow<ChoiceSet> =
        preferences.choicesChanges().stateIn(viewModelScope, SharingStarted.Eagerly, preferences.choices())

    val profiles: StateFlow<List<ProfileSync>> =
        profileStates.profiles().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val isLinked: StateFlow<Boolean> =
        connection.states().map { it.isLinked }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** The area being turned back on that waits for the person to say whose settings it keeps. */
    val askingArea: StateFlow<SettingsArea?>
        field = MutableStateFlow<SettingsArea?>(null)

    /** The account profile being switched off, which waits for the person to confirm it leaves this device. */
    val removingProfile: StateFlow<ProfileSync?>
        field = MutableStateFlow<ProfileSync?>(null)

    fun syncData(group: DataGroup, on: Boolean) {
        appScope.launchIO { changes.syncData(group, on) }
    }

    fun syncArea(area: SettingsArea, on: Boolean) {
        appScope.launchIO {
            if (on && changes.asksWhoseSettings(area)) {
                askingArea.value = area
            } else {
                changes.syncArea(area, on)
            }
        }
    }

    fun answerArea(keep: SettingsWinner) {
        val area = askingArea.value ?: return
        askingArea.value = null
        appScope.launchIO { changes.syncArea(area, on = true, keep = keep) }
    }

    fun dismissAreaQuestion() {
        askingArea.value = null
    }

    fun syncSetting(key: String, on: Boolean) {
        appScope.launchIO { changes.syncSetting(key, on) }
    }

    /** Turns syncing [profile] on, or off, first asking when that takes it off this device. */
    fun syncProfile(profile: ProfileSync, on: Boolean) {
        appScope.launchIO {
            if (!on && profile.isHere && changes.leavesThisDevice(profile.uuid)) {
                removingProfile.value = profile
            } else {
                changes.syncProfile(profile.uuid, on)
            }
        }
    }

    /**
     * Takes the profile being switched off off this device, once [switchAway] moved to another profile where it is
     * the open one; nothing changes when the switch didn't happen. The switch may close this screen, so the removal
     * doesn't depend on it staying open.
     */
    fun removeProfile(switchAway: (suspend () -> Boolean)?) {
        val profile = removingProfile.value ?: return
        removingProfile.value = null
        appScope.launchUI {
            if (switchAway != null && !switchAway()) return@launchUI
            changes.syncProfile(profile.uuid, on = false)
        }
    }

    fun dismissProfileRemoval() {
        removingProfile.value = null
    }
}

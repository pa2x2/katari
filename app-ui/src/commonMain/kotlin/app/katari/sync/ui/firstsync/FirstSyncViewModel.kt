package app.katari.sync.ui.firstsync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.katari.profiles.core.ProfileManager
import app.katari.sync.client.account.AccountCall
import app.katari.sync.client.account.SyncAccount
import app.katari.sync.client.firstsync.ProfileChoice
import app.katari.sync.client.firstsync.ProfilePairing
import app.katari.sync.client.firstsync.SettingsWinner
import app.katari.sync.client.part.ChoiceSet
import app.katari.sync.client.preference.SyncPreferences
import app.katari.ui.di.AppUiScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import tachiyomi.core.common.util.lang.launchIO

/**
 * The first-sync choices of a device that has data joining an account that has too (D027, D047): each profile here is
 * proposed as the account profile of the same name, or added as new where none matches, and each paired profile takes
 * the account's settings unless the person says otherwise. The default profiles always pair, as does a profile that
 * already is one of the account's; any other may stay on this device unsynced. What else syncs is this device's
 * choice ([syncChoices]), made on What syncs.
 */
@Inject
@ViewModelKey
@ContributesIntoMap(AppUiScope::class)
class FirstSyncViewModel(
    private val account: SyncAccount,
    profileManager: ProfileManager,
    private val preferences: SyncPreferences,
) : ViewModel() {
    val profiles = profileManager.profiles

    val syncChoices: StateFlow<ChoiceSet> =
        preferences.choicesChanges().stateIn(viewModelScope, SharingStarted.Eagerly, preferences.choices())

    val state: StateFlow<State>
        field = MutableStateFlow<State>(State.Loading)

    init {
        load()
    }

    fun load() {
        state.value = State.Loading
        viewModelScope.launchIO {
            state.value = when (val call = account.firstSyncProposal()) {
                is AccountCall.Done -> State.Choosing(
                    pairings = call.value,
                    choices = call.value.associate { it.profileId to Choice(proposedJoin(it)) },
                )
                else -> State.Unavailable
            }
        }
    }

    /** Pairs where a profile of the account matches; a profile already switched off on What syncs stays here. */
    private fun proposedJoin(pairing: ProfilePairing): Join = when {
        pairing.canStayHere && pairing.uuid in preferences.profilesOff.get() -> Join.ONLY_HERE
        pairing.pairedWith != null -> Join.PAIRED
        else -> Join.NEW
    }

    fun setJoin(profileId: Long, join: Join) = change(profileId) { it.copy(join = join) }

    fun setSettings(profileId: Long, settings: SettingsWinner) = change(profileId) { it.copy(settings = settings) }

    /** Starts the first sync with the choices made; it runs in the background from here (D027). */
    fun start() {
        val choosing = state.value as? State.Choosing ?: return
        state.value = State.Starting
        viewModelScope.launchIO {
            account.answerFirstSync(choosing.answer())
            state.value = State.Started
        }
    }

    private fun change(profileId: Long, update: (Choice) -> Choice) {
        state.update { current ->
            if (current !is State.Choosing) return@update current
            val choice = current.choices[profileId] ?: return@update current
            current.copy(choices = current.choices + (profileId to update(choice)))
        }
    }

    /** How one of this device's profiles joins the account. */
    enum class Join {
        /** As the account profile proposed for it. */
        PAIRED,

        /** As a new profile of the account. */
        NEW,

        /** It doesn't: it stays on this device, unsynced. */
        ONLY_HERE,
    }

    data class Choice(val join: Join, val settings: SettingsWinner = SettingsWinner.ACCOUNT)

    sealed interface State {
        data object Loading : State

        data class Choosing(val pairings: List<ProfilePairing>, val choices: Map<Long, Choice>) : State {
            fun joinOf(pairing: ProfilePairing): Join =
                if (pairing.sameProfile) Join.PAIRED else choices.getValue(pairing.profileId).join

            fun answer(): List<ProfileChoice> = pairings.map { pairing ->
                val join = joinOf(pairing)
                ProfileChoice(
                    profileId = pairing.profileId,
                    accountUuid = pairing.pairedWith?.uuid?.takeIf { join == Join.PAIRED },
                    settings = choices.getValue(pairing.profileId).settings,
                    onlyHere = join == Join.ONLY_HERE,
                )
            }
        }

        data object Starting : State

        data object Started : State

        data object Unavailable : State
    }
}

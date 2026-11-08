package app.katari.sync.ui.firstsync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.katari.sync.client.account.AccountCall
import app.katari.sync.client.account.SyncAccount
import app.katari.sync.client.firstsync.ProfileChoice
import app.katari.sync.client.firstsync.ProfilePairing
import app.katari.sync.client.firstsync.SettingsWinner
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import mihon.app.di.AppUiScope
import mihon.feature.profiles.core.ProfileManager
import tachiyomi.core.common.util.lang.launchIO

/**
 * The first-sync choices of a device that has data joining an account that has too (D027, D047): each profile here is
 * proposed as the account profile of the same name, or added as new where none matches, and each paired profile takes
 * the account's settings unless the person says otherwise. The default profiles always pair, as does a profile that
 * already is one of the account's.
 */
@Inject
@ViewModelKey
@ContributesIntoMap(AppUiScope::class)
class FirstSyncViewModel(private val account: SyncAccount, profileManager: ProfileManager) : ViewModel() {
    val profiles = profileManager.profiles

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
                    choices = call.value.associate { it.profileId to Choice(paired = it.pairedWith != null) },
                )
                else -> State.Unavailable
            }
        }
    }

    fun setPaired(profileId: Long, paired: Boolean) = change(profileId) { it.copy(paired = paired) }

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

    data class Choice(val paired: Boolean, val settings: SettingsWinner = SettingsWinner.ACCOUNT)

    sealed interface State {
        data object Loading : State

        data class Choosing(val pairings: List<ProfilePairing>, val choices: Map<Long, Choice>) : State {
            fun isPaired(pairing: ProfilePairing) = pairing.sameProfile || choices.getValue(pairing.profileId).paired

            fun answer(): List<ProfileChoice> = pairings.map { pairing ->
                val pairedWith = pairing.pairedWith?.takeIf { isPaired(pairing) }
                ProfileChoice(pairing.profileId, pairedWith?.uuid, choices.getValue(pairing.profileId).settings)
            }
        }

        data object Starting : State

        data object Started : State

        data object Unavailable : State
    }
}

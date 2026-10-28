package eu.kanade.tachiyomi.ui.base.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import tachiyomi.core.common.util.lang.SubscriberGate
import tachiyomi.core.common.util.lang.pausedWhileUnsubscribed
import kotlin.time.Duration.Companion.seconds

/**
 * A ViewModel for a screen that stays alive while hidden, such as a home tab. Its collectors keep running,
 * but database queries and other flows marked with [pausedWhileUnsubscribed] hold back their updates while
 * nothing collects [state], so a hidden screen doesn't re-query on every write and shows its last state at once
 * when it returns.
 *
 * Collect data in [gatedScope] and keep the model's own reads and derived flows on [mutableState]: only collectors
 * of [state] count as subscribers, and [state] trails [mutableState] by a dispatch when updated off the main
 * thread. Screens must render from [state]; one that renders some other flow of this model would see it stop
 * updating once [state] has had no collector for a few seconds.
 */
abstract class GatedStateViewModel<S>(initialState: S) : ViewModel() {

    private val subscriberGate = SubscriberGate()

    protected val gatedScope: CoroutineScope = viewModelScope + subscriberGate

    protected val mutableState: MutableStateFlow<S> = MutableStateFlow(initialState)

    val state: StateFlow<S> = mutableState.stateIn(
        gatedScope,
        subscriberGate.whileSubscribed(5.seconds),
        initialState,
    )
}

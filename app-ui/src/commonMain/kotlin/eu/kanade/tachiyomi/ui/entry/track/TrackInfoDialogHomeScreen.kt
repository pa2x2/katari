package eu.kanade.tachiyomi.ui.entry.track

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLocale
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.track.TrackInfoDialogHome
import eu.kanade.presentation.util.Screen
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.LogPriority
import mihon.app.di.AppUiScope
import mihon.app.ui.di.LocalAppUiGraph
import mihon.core.platform.PlatformContext
import mihon.core.platform.currentPlatformContext
import mihon.entry.interactions.tracking.EntryTrackingAutomaticRegistrationResult
import mihon.entry.interactions.tracking.EntryTrackingFeature
import mihon.entry.interactions.tracking.EntryTrackingMutation
import mihon.entry.interactions.tracking.EntryTrackingRefreshFailure
import mihon.entry.interactions.tracking.EntryTrackingRefreshResult
import mihon.entry.interactions.tracking.EntryTrackingSession
import mihon.entry.interactions.tracking.EntryTrackingSessionService
import mihon.presentation.kit.feedback.rememberScreenMessages
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.model.Entry
import tachiyomi.i18n.*
import tachiyomi.i18n.platform.stringResource
import java.time.format.DateTimeFormatter
import java.util.Locale

data class TrackInfoDialogHomeScreen(
    private val entry: Entry,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = currentPlatformContext()
        val graph = LocalAppUiGraph.current
        val viewModel = assistedMetroViewModel<Model, Model.Factory> { create(entry) }
        val locale = LocalLocale.current.platformLocale
        val dateFormat = remember(locale) { viewModel.dateFormat(locale) }
        val state by viewModel.state.collectAsState()
        val messages = rememberScreenMessages()
        LaunchedEffect(viewModel) {
            viewModel.messages.collect(messages::show)
        }

        TrackInfoDialogHome(
            trackItems = state.trackItems,
            dateFormat = dateFormat,
            onStatusClick = {
                navigator.push(TrackStatusSelectorScreen(entry, it.track!!, it.service.id))
            },
            onChapterClick = {
                navigator.push(TrackProgressSelectorScreen(entry, it.track!!, it.service.id))
            },
            onScoreClick = {
                navigator.push(TrackScoreSelectorScreen(entry, it.service.id, it.displayScore.orEmpty()))
            },
            onStartDateEdit = {
                navigator.push(TrackDateSelectorScreen(entry, it.track!!, it.service.id, start = true))
            },
            onEndDateEdit = {
                navigator.push(TrackDateSelectorScreen(entry, it.track!!, it.service.id, start = false))
            },
            onNewSearch = {
                if (it.service.capabilities.supportsAutomaticBinding) {
                    viewModel.registerAutomatically(it)
                } else {
                    navigator.push(
                        TrackerSearchScreen(
                            entry = entry,
                            initialQuery = it.track?.title ?: entry.displayTitle,
                            currentUrl = it.track?.remoteUrl,
                            serviceId = it.service.id,
                        ),
                    )
                }
            },
            onOpenInBrowser = { item -> item.remoteUrl()?.let { graph.linkOpener.open(context, it) } },
            onRemoved = {
                navigator.push(TrackerRemoveScreen(entry, it.service.id))
            },
            onCopyLink = { item -> item.remoteUrl()?.let { graph.textClipboard.copy(it, it) } },
            onTogglePrivate = viewModel::togglePrivate,
        )
    }

    private fun EntryTrackingSessionService.remoteUrl(): String? = track?.remoteUrl?.takeIf(String::isNotBlank)

    @AssistedInject
    class Model(
        @Assisted private val entry: Entry,
        private val trackingFeature: EntryTrackingFeature,
        private val uiPreferences: UiPreferences,
        private val context: PlatformContext,
    ) : ViewModel() {

        private val _messages = Channel<String>(Channel.UNLIMITED)

        /** Outcomes to tell the user about, shown on the sheet. */
        val messages: Flow<String> = _messages.receiveAsFlow()

        @AssistedFactory
        @ManualViewModelAssistedFactoryKey
        @ContributesIntoMap(AppUiScope::class)
        interface Factory : ManualViewModelAssistedFactory {
            fun create(entry: Entry): Model
        }

        val state: StateFlow<Model.State>
            field = MutableStateFlow<Model.State>(State())

        init {
            viewModelScope.launch { refreshTrackers() }
            viewModelScope.launch {
                trackingFeature.observeSession(entry).collectLatest { session ->
                    val services = (session as? EntryTrackingSession.Available)?.services.orEmpty()
                    state.update { it.copy(trackItems = services) }
                }
            }
        }

        fun dateFormat(locale: Locale): DateTimeFormatter =
            UiPreferences.dateFormat(uiPreferences.dateFormat.get(), locale)

        fun registerAutomatically(item: EntryTrackingSessionService) {
            viewModelScope.launchNonCancellable {
                when (trackingFeature.registerAutomatically(entry, item.service.id)) {
                    EntryTrackingAutomaticRegistrationResult.Registered -> Unit
                    EntryTrackingAutomaticRegistrationResult.NoMatch,
                    is EntryTrackingAutomaticRegistrationResult.Unavailable,
                    is EntryTrackingAutomaticRegistrationResult.Failed,
                    -> _messages.send(context.stringResource(MR.strings.error_no_match))
                }
            }
        }

        fun togglePrivate(item: EntryTrackingSessionService) {
            viewModelScope.launchNonCancellable {
                trackingFeature.mutate(
                    entry = entry,
                    serviceId = item.service.id,
                    mutation = EntryTrackingMutation.Private(!item.track!!.private),
                ).logFailure("privacy update")
            }
        }

        private suspend fun refreshTrackers() {
            when (val result = trackingFeature.refresh(entry)) {
                is EntryTrackingRefreshResult.Completed -> {
                    result.failures.forEach { failure -> reportRefreshFailure(failure) }
                }
                is EntryTrackingRefreshResult.Failed -> logcat(LogPriority.ERROR, result.cause) {
                    "Failed to refresh track data entryId=${entry.id}"
                }
                is EntryTrackingRefreshResult.Unavailable -> Unit
            }
        }

        private suspend fun reportRefreshFailure(failure: EntryTrackingRefreshFailure) {
            logcat(LogPriority.ERROR, failure.cause) {
                "Failed to refresh track data entryId=${entry.id} for service ${failure.serviceId.value}"
            }
            _messages.send(
                context.stringResource(
                    MR.strings.track_error,
                    failure.serviceName,
                    failure.cause.message.orEmpty(),
                ),
            )
        }

        @Immutable
        data class State(
            val trackItems: List<EntryTrackingSessionService> = emptyList(),
        )
    }
}

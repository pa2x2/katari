package eu.kanade.tachiyomi.ui.entry.track

import android.app.Application
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.track.TrackInfoDialogHome
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.util.system.copyToClipboard
import eu.kanade.tachiyomi.util.system.openInBrowser
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.LogPriority
import mihon.entry.interactions.EntryTrackingAutomaticRegistrationResult
import mihon.entry.interactions.EntryTrackingFeature
import mihon.entry.interactions.EntryTrackingMutation
import mihon.entry.interactions.EntryTrackingRefreshResult
import mihon.entry.interactions.EntryTrackingSession
import mihon.entry.interactions.EntryTrackingSessionService
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.model.Entry
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

data class TrackInfoDialogHomeScreen(
    private val entry: Entry,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val screenModel = rememberScreenModel { Model(entry) }
        val dateFormat = remember { UiPreferences.dateFormat(Injekt.get<UiPreferences>().dateFormat.get()) }
        val state by screenModel.state.collectAsState()

        TrackInfoDialogHome(
            trackItems = state.trackItems,
            dateFormat = dateFormat,
            onStatusClick = {
                navigator.push(TrackStatusSelectorScreen(entry, it.track!!, it.service))
            },
            onChapterClick = {
                navigator.push(TrackProgressSelectorScreen(entry, it.track!!, it.service.id))
            },
            onScoreClick = {
                navigator.push(TrackScoreSelectorScreen(entry, it.service, it.displayScore.orEmpty()))
            },
            onStartDateEdit = {
                navigator.push(TrackDateSelectorScreen(entry, it.track!!, it.service, start = true))
            },
            onEndDateEdit = {
                navigator.push(TrackDateSelectorScreen(entry, it.track!!, it.service, start = false))
            },
            onNewSearch = {
                if (it.service.capabilities.supportsAutomaticBinding) {
                    screenModel.registerAutomatically(it)
                } else {
                    navigator.push(
                        TrackerSearchScreen(
                            entry = entry,
                            initialQuery = it.track?.title ?: entry.displayTitle,
                            currentUrl = it.track?.remoteUrl,
                            service = it.service,
                        ),
                    )
                }
            },
            onOpenInBrowser = { openTrackerInBrowser(context, it) },
            onRemoved = {
                navigator.push(TrackerRemoveScreen(entry, it.service))
            },
            onCopyLink = { context.copyTrackerLink(it) },
            onTogglePrivate = screenModel::togglePrivate,
        )
    }

    private fun openTrackerInBrowser(context: Context, item: EntryTrackingSessionService) {
        item.track?.remoteUrl?.takeIf(String::isNotBlank)?.let(context::openInBrowser)
    }

    private fun Context.copyTrackerLink(item: EntryTrackingSessionService) {
        item.track?.remoteUrl?.takeIf(String::isNotBlank)?.let { copyToClipboard(it, it) }
    }

    private class Model(
        private val entry: Entry,
        private val trackingFeature: EntryTrackingFeature = Injekt.get(),
    ) : StateScreenModel<Model.State>(State()) {

        init {
            screenModelScope.launch { refreshTrackers() }
            screenModelScope.launch {
                trackingFeature.observeSession(entry).collectLatest { session ->
                    val services = (session as? EntryTrackingSession.Available)?.services.orEmpty()
                    mutableState.update { it.copy(trackItems = services) }
                }
            }
        }

        fun registerAutomatically(item: EntryTrackingSessionService) {
            screenModelScope.launchNonCancellable {
                when (trackingFeature.registerAutomatically(entry, item.service.id)) {
                    EntryTrackingAutomaticRegistrationResult.Registered -> Unit
                    EntryTrackingAutomaticRegistrationResult.NoMatch,
                    is EntryTrackingAutomaticRegistrationResult.Unavailable,
                    is EntryTrackingAutomaticRegistrationResult.Failed,
                    -> withUIContext { Injekt.get<Application>().toast(MR.strings.error_no_match) }
                }
            }
        }

        fun togglePrivate(item: EntryTrackingSessionService) {
            screenModelScope.launchNonCancellable {
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

        private suspend fun reportRefreshFailure(failure: mihon.entry.interactions.EntryTrackingRefreshFailure) {
            logcat(LogPriority.ERROR, failure.cause) {
                "Failed to refresh track data entryId=${entry.id} for service ${failure.serviceId.value}"
            }
            withUIContext {
                val context = Injekt.get<Application>()
                context.toast(
                    context.stringResource(
                        MR.strings.track_error,
                        failure.serviceName,
                        failure.cause.message.orEmpty(),
                    ),
                )
            }
        }

        @Immutable
        data class State(
            val trackItems: List<EntryTrackingSessionService> = emptyList(),
        )
    }
}

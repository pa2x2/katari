package eu.kanade.tachiyomi.ui.entry.track

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import eu.kanade.presentation.track.TrackRemovalConfirmation
import eu.kanade.presentation.util.Screen
import mihon.app.di.AppUiScope
import mihon.entry.interactions.tracking.EntryTrackingFeature
import mihon.entry.interactions.tracking.EntryTrackingServiceDescriptor
import mihon.entry.interactions.tracking.EntryTrackingServiceId
import mihon.presentation.kit.list.CheckRow
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.domain.entry.model.Entry
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

internal data class TrackerRemoveScreen(
    private val entry: Entry,
    private val serviceId: EntryTrackingServiceId,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = assistedMetroViewModel<Model, Model.Factory> { create(entry, serviceId) }
        val service = viewModel.service
        var removeRemoteTrack by remember { mutableStateOf(false) }
        TrackRemovalConfirmation(
            title = stringResource(MR.strings.track_delete_title, service.name),
            text = stringResource(MR.strings.track_delete_text, service.name),
            confirmLabel = stringResource(MR.strings.action_remove),
            onConfirm = {
                viewModel.remove(removeRemoteTrack)
                navigator.pop()
            },
            onDismissRequest = navigator::pop,
            options = if (service.capabilities.supportsRemoteDeletion) {
                {
                    CheckRow(
                        title = stringResource(MR.strings.track_delete_remote_text, service.name),
                        checked = removeRemoteTrack,
                        onCheckedChange = { removeRemoteTrack = it },
                    )
                }
            } else {
                null
            },
        )
    }

    @AssistedInject
    class Model(
        @Assisted private val entry: Entry,
        @Assisted private val serviceId: EntryTrackingServiceId,
        private val trackingFeature: EntryTrackingFeature,
    ) : ViewModel() {

        @AssistedFactory
        @ManualViewModelAssistedFactoryKey
        @ContributesIntoMap(AppUiScope::class)
        interface Factory : ManualViewModelAssistedFactory {
            fun create(entry: Entry, serviceId: EntryTrackingServiceId): Model
        }

        val service: EntryTrackingServiceDescriptor = checkNotNull(trackingFeature.service(serviceId))

        fun remove(removeRemote: Boolean) {
            viewModelScope.launchNonCancellable {
                trackingFeature.remove(entry, serviceId, removeRemote).logFailure()
            }
        }
    }
}

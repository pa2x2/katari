package eu.kanade.tachiyomi.ui.entry.track

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.track.TrackRemovalConfirmation
import eu.kanade.presentation.util.Screen
import mihon.entry.interactions.tracking.EntryTrackingFeature
import mihon.entry.interactions.tracking.EntryTrackingServiceDescriptor
import mihon.presentation.kit.list.CheckRow
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.domain.entry.model.Entry
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal data class TrackerRemoveScreen(
    private val entry: Entry,
    private val service: EntryTrackingServiceDescriptor,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = viewModel { Model(entry, service) }
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

    private class Model(
        private val entry: Entry,
        private val service: EntryTrackingServiceDescriptor,
        private val trackingFeature: EntryTrackingFeature = Injekt.get(),
    ) : ViewModel() {

        fun remove(removeRemote: Boolean) {
            viewModelScope.launchNonCancellable {
                trackingFeature.remove(entry, service.id, removeRemote).logFailure()
            }
        }
    }
}

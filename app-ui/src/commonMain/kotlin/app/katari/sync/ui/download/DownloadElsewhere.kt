package app.katari.sync.ui.download

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.katari.presentation.kit.feedback.rememberScreenMessages
import app.katari.presentation.kit.sheet.PopoverAnchor
import app.katari.sync.client.downloadrequest.ChapterToRequest
import app.katari.sync.client.downloadrequest.DownloadTarget
import app.katari.sync.client.downloadrequest.DownloadTargetReadiness
import app.katari.sync.protocol.device.DeviceId
import app.katari.sync.protocol.device.LinkedDevice
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.launch
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The screen's [DownloadElsewhere], or null on a screen that doesn't offer it. */
val LocalDownloadElsewhere = staticCompositionLocalOf<DownloadElsewhere?> { null }

/**
 * Where chapters can be offered for another device of the account to download: [deviceName] is the only device that
 * can take them, which the action then names, or null when the person picks among several.
 */
@Stable
class DownloadElsewhereOffer(val deviceName: String?)

/** The action's label: naming the only device that can take the chapters, or leading to the picker. */
@Composable
fun DownloadElsewhereOffer.label(): String =
    deviceName?.let { stringResource(MR.strings.sync_download_on, it) }
        ?: stringResource(MR.strings.sync_download_elsewhere)

/** The label of the last of an entry's Download amounts, which asks for the device first. */
@Composable
fun DownloadElsewhereOffer.amountsLabel(): String = deviceName
    ?.let { stringResource(MR.strings.sync_download_elsewhere_amounts_on, it) }
    ?: stringResource(MR.strings.sync_download_elsewhere_amounts)

/**
 * "Download on another device" for one screen's chapters: whether to offer it for some, and the device picker it
 * opens. A device that can't take download requests yet is listed but not offered, and when only one device can, the
 * action names it and sends without asking, unless that device lacks the chapters' extension.
 */
@Stable
class DownloadElsewhere internal constructor(
    internal val viewModel: DownloadElsewhereViewModel,
    private val devices: State<List<DownloadTarget>>,
    private val syncing: State<Set<Long>>,
) {
    internal var pick by mutableStateOf<DownloadElsewherePick?>(null)

    // The snackbar each device's request is confirmed with, worded in composition
    internal var sentMessages: Map<DeviceId, String> = emptyMap()
    internal var showMessage: (String) -> Unit = {}

    internal val listedDevices: List<DownloadTarget> get() = devices.value

    private val able: List<LinkedDevice>
        get() = devices.value.filter { it.readiness != DownloadTargetReadiness.NEEDS_NEWER_APP }.map { it.device }

    /** How to offer chapters of [entryIds] to another device; null when none can take them or none of them syncs. */
    fun offer(entryIds: Collection<Long>): DownloadElsewhereOffer? {
        val syncingNow = syncing.value
        if (entryIds.none { it in syncingNow }) return null
        val able = able
        return when (able.size) {
            0 -> null
            1 -> DownloadElsewhereOffer(able.single().description.name)
            else -> DownloadElsewhereOffer(null)
        }
    }

    /** Opens the picker for [request] beside [anchor]'s button, or sends it at once to the only device that can take it. */
    fun open(request: DownloadElsewhereRequest, anchor: PopoverAnchor?) {
        val only = able.singleOrNull()
        if (only == null) {
            pick = DownloadElsewherePick.Devices(request, anchor)
            return
        }
        viewModel.viewModelScope.launch {
            val readiness = viewModel.currentDevicesFor(request.sourceId).targets
                .firstOrNull { it.device.id == only.id }
                ?.readiness
            // Picking shows why it can't take them
            pick = if (readiness != DownloadTargetReadiness.READY) {
                DownloadElsewherePick.Devices(request, anchor)
            } else {
                when (request) {
                    is DownloadElsewhereRequest.Amounts ->
                        DownloadElsewherePick.Amounts(request, anchor, only, canGoBack = false)
                    is DownloadElsewhereRequest.Chapters -> {
                        send(only, request)
                        null
                    }
                }
            }
        }
    }

    internal fun picked(device: LinkedDevice) {
        val current = pick ?: return
        when (val request = current.request) {
            is DownloadElsewhereRequest.Amounts ->
                pick = DownloadElsewherePick.Amounts(request, current.anchor, device, canGoBack = true)
            is DownloadElsewhereRequest.Chapters -> {
                pick = null
                send(device, request)
            }
        }
    }

    internal fun pickedAmount(device: LinkedDevice, amount: DownloadElsewhereAmount) {
        pick = null
        sendChapters(device) { amount.chapters() }
    }

    internal fun back() {
        val current = pick as? DownloadElsewherePick.Amounts ?: return
        pick = DownloadElsewherePick.Devices(current.request, current.anchor)
    }

    private fun send(device: LinkedDevice, request: DownloadElsewhereRequest.Chapters) =
        sendChapters(device) { request.chapters }

    private fun sendChapters(device: LinkedDevice, chapters: suspend () -> List<ChapterToRequest>) {
        val message = sentMessages[device.id]
        viewModel.send(device, chapters) { message?.let(showMessage) }
    }
}

/** The step the picker is at: choosing the device, or, for an entry's Download menu, how much it downloads. */
internal sealed interface DownloadElsewherePick {
    val request: DownloadElsewhereRequest
    val anchor: PopoverAnchor?

    class Devices(override val request: DownloadElsewhereRequest, override val anchor: PopoverAnchor?) :
        DownloadElsewherePick

    class Amounts(
        override val request: DownloadElsewhereRequest.Amounts,
        override val anchor: PopoverAnchor?,
        val device: LinkedDevice,
        val canGoBack: Boolean,
    ) : DownloadElsewherePick
}

/** Offers "Download on another device" on [content]'s chapters, through [LocalDownloadElsewhere]. */
@Composable
fun ProvideDownloadElsewhere(content: @Composable () -> Unit) {
    val viewModel = metroViewModel<DownloadElsewhereViewModel>()
    val devices = viewModel.devices.collectAsStateWithLifecycle()
    val syncing = viewModel.syncing.collectAsStateWithLifecycle()
    val elsewhere = remember(viewModel) { DownloadElsewhere(viewModel, devices, syncing) }
    val messages = rememberScreenMessages()
    val sentMessages = devices.value.associate { target ->
        val name = target.device.description.name
        target.device.id to if (target.device.connected) {
            stringResource(MR.strings.sync_download_requested, name)
        } else {
            stringResource(MR.strings.sync_download_requested_later, name)
        }
    }
    SideEffect {
        elsewhere.sentMessages = sentMessages
        elsewhere.showMessage = { messages.show(it) }
    }
    CompositionLocalProvider(LocalDownloadElsewhere provides elsewhere) {
        content()
        elsewhere.pick?.let { DownloadElsewherePicker(it, elsewhere) }
    }
}

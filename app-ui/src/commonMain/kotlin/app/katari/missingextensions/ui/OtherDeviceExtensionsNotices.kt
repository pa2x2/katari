package app.katari.missingextensions.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Devices
import app.katari.missingextensions.otherdevices.OtherDeviceChanges
import app.katari.presentation.kit.surface.NoticeCard
import app.katari.presentation.kit.surface.NoticeTone
import dev.zacsweers.metrox.viewmodel.metroViewModel
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The Library's offer to install and uninstall here what the account's other devices did, shown once for each such
 * change; Not now and Review both put it away until another one comes.
 */
@Composable
fun OtherDeviceExtensionsLibraryNotice(onReview: () -> Unit, modifier: Modifier = Modifier) {
    val model = metroViewModel<OtherDeviceExtensionsNoticeViewModel>()
    val changes by model.libraryCard.collectAsStateWithLifecycle()
    if (changes.all.isEmpty()) return
    NoticeCard(
        text = noticeText(changes),
        tone = NoticeTone.Info,
        modifier = modifier,
        icon = MaterialSymbols.Rounded.Devices,
        actionLabel = stringResource(MR.strings.action_review),
        onAction = {
            model.answerLibraryCard()
            onReview()
        },
        dismissLabel = stringResource(MR.strings.action_not_now),
        onDismiss = model::answerLibraryCard,
    )
}

/** Browse → Extensions' way back to the offer, shown while anything is offered, whatever the Library card did. */
@Composable
fun OtherDeviceExtensionsNotice(onReview: () -> Unit, modifier: Modifier = Modifier) {
    val model = metroViewModel<OtherDeviceExtensionsNoticeViewModel>()
    val changes by model.offered.collectAsStateWithLifecycle()
    if (changes.all.isEmpty()) return
    NoticeCard(
        text = noticeText(changes),
        tone = NoticeTone.Info,
        modifier = modifier,
        icon = MaterialSymbols.Rounded.Devices,
        actionLabel = stringResource(MR.strings.action_review),
        onAction = onReview,
    )
}

/** What the other devices did, naming the device when one did it all. */
@Composable
private fun noticeText(changes: OtherDeviceChanges): String {
    val device = changes.all.map { it.change.deviceName }.distinct().singleOrNull()
        ?: stringResource(MR.strings.other_device_extensions_your_devices)
    val installs = changes.installs.size
    val uninstalls = changes.uninstalls.size
    return when {
        uninstalls == 0 ->
            pluralStringResource(MR.plurals.other_device_extensions_card_installed, installs, device, installs)
        installs == 0 ->
            pluralStringResource(MR.plurals.other_device_extensions_card_uninstalled, uninstalls, device, uninstalls)
        else -> stringResource(MR.strings.other_device_extensions_card_both, device)
    }
}

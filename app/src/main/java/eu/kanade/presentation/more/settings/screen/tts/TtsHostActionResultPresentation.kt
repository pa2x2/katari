package eu.kanade.presentation.more.settings.screen.tts

import android.content.Context
import eu.kanade.tachiyomi.util.system.toast
import mihon.tts.api.host.TtsHostActionResult
import mihon.tts.api.host.TtsSetupDestination
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR

internal fun Context.presentTtsHostActionResult(result: TtsHostActionResult) {
    when (result) {
        TtsHostActionResult.Completed -> Unit
        is TtsHostActionResult.SetupOpened -> if (result.destination == TtsSetupDestination.External) {
            toast(MR.strings.tts_settings_external_setup_opened)
        }
        TtsHostActionResult.SetupUnsupported -> toast(MR.strings.tts_settings_setup_unsupported)
        TtsHostActionResult.ServiceMissing,
        TtsHostActionResult.SettingsUnavailable,
        -> toast(MR.strings.tts_settings_setup_unavailable)
        is TtsHostActionResult.Failed -> toast(
            stringResource(MR.strings.tts_settings_setup_failed, result.reason),
        )
    }
}

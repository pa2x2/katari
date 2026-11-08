package eu.kanade.presentation.more.settings.screen.tts

import app.katari.core.platform.PlatformContext
import app.katari.presentation.kit.feedback.ScreenMessages
import app.katari.tts.api.host.TtsHostActionResult
import app.katari.tts.api.host.TtsSetupDestination
import tachiyomi.i18n.*
import tachiyomi.i18n.platform.stringResource

internal fun ScreenMessages.presentTtsHostActionResult(context: PlatformContext, result: TtsHostActionResult) {
    when (result) {
        TtsHostActionResult.Completed -> Unit
        is TtsHostActionResult.SetupOpened -> if (result.destination == TtsSetupDestination.External) {
            show(MR.strings.tts_settings_external_setup_opened)
        }
        TtsHostActionResult.SetupUnsupported -> show(MR.strings.tts_settings_setup_unsupported)
        TtsHostActionResult.ServiceMissing,
        TtsHostActionResult.SettingsUnavailable,
        -> show(MR.strings.tts_settings_setup_unavailable)
        is TtsHostActionResult.Failed -> show(
            context.stringResource(MR.strings.tts_settings_setup_failed, result.reason),
        )
    }
}

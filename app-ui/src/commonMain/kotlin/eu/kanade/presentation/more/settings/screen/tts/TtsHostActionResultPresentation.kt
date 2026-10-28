package eu.kanade.presentation.more.settings.screen.tts

import mihon.core.platform.PlatformContext
import mihon.presentation.kit.feedback.ScreenMessages
import mihon.tts.api.host.TtsHostActionResult
import mihon.tts.api.host.TtsSetupDestination
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

package eu.kanade.presentation.more.settings.screen.tts

import mihon.tts.api.host.TtsHostActions

/** Graph entry points of the speech settings beyond their model, such as whether the settings list offers them. */
interface TtsSettingsGraph {
    val ttsHostActions: TtsHostActions
}

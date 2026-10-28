package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import mihon.desktop.tts.NoTtsFeature
import mihon.desktop.tts.NoTtsHostActions
import mihon.tts.api.TtsFeature
import mihon.tts.api.host.TtsHostActions

@BindingContainer
interface SpeechBindings {
    @Binds
    val NoTtsFeature.ttsFeature: TtsFeature

    @Binds
    val NoTtsHostActions.ttsHostActions: TtsHostActions
}

package mihon.tts.runtime.audio

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

@ContributesTo(AppScope::class)
@BindingContainer
object DesktopTtsAudioFocusBindings {
    @Provides
    internal fun audioFocus(): TtsAudioFocus = NoTtsAudioFocus
}

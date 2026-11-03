package mihon.tts.runtime.audio

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

@ContributesTo(AppScope::class)
@BindingContainer
object AndroidTtsAudioFocusBindings {
    @Provides
    internal fun audioFocus(application: Application): TtsAudioFocus = AndroidTtsAudioFocus(application)
}

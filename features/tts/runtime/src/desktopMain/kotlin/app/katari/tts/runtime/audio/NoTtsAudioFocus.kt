package app.katari.tts.runtime.audio

/** Desktops have no audio focus that one app takes from another, so speech plays alongside other audio. */
internal object NoTtsAudioFocus : TtsAudioFocus {
    override val isOffered: Boolean = false

    override fun request(onLost: () -> Unit): Boolean = false

    override fun abandon() = Unit
}

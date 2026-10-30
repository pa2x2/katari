package mihon.tts.runtime.audio

import mihon.core.common.platform.PlatformFacility

/** The platform's audio focus, which speech takes while it plays so that other audio pauses or quiets. */
@PlatformFacility
interface TtsAudioFocus {
    /** False where the platform has no audio focus; speech then plays without taking one. */
    val isOffered: Boolean

    /** Takes the focus; [onLost] runs when another app takes it away. False when the platform refuses it. */
    fun request(onLost: () -> Unit): Boolean

    fun abandon()
}

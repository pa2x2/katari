package mihon.tts.provider.android.connection

import android.speech.tts.Voice

internal class AndroidTtsVoiceSnapshot(
    val voices: Collection<Voice>,
    val defaultVoice: Voice?,
) {
    private val voicesByName = voices.associateBy(Voice::getName)

    fun find(name: String): Voice? = voicesByName[name]
}

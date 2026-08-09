package mihon.tts.runtime.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper

internal interface TtsAudioFocus {
    fun request(): Boolean

    fun abandon()
}

internal class AndroidTtsAudioFocus(
    private val context: Context,
    private val onFocusLost: () -> Unit,
) : TtsAudioFocus {
    private val audioManager by lazy { context.getSystemService(AudioManager::class.java) }
    private val request by lazy {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            .setWillPauseWhenDucked(true)
            .setOnAudioFocusChangeListener(::onFocusChanged, Handler(Looper.getMainLooper()))
            .build()
    }

    @Volatile
    private var held = false

    override fun request(): Boolean {
        return try {
            (audioManager?.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED).also { granted ->
                held = granted
            }
        } catch (_: RuntimeException) {
            held = false
            false
        }
    }

    override fun abandon() {
        if (!held) return
        held = false
        try {
            audioManager?.abandonAudioFocusRequest(request)
        } catch (_: RuntimeException) {
            // The focus is already treated as abandoned locally.
        }
    }

    private fun onFocusChanged(change: Int) {
        if (change == AudioManager.AUDIOFOCUS_LOSS ||
            change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ||
            change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK
        ) {
            held = false
            onFocusLost()
        }
    }
}

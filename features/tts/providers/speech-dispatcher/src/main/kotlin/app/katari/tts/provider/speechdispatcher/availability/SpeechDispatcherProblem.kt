package app.katari.tts.provider.speechdispatcher.availability

import tachiyomi.i18n.*
import tachiyomi.i18n.localized.stringResource

/** Why Speech Dispatcher can't speak, in words for the user, who has to fix it outside Katari. */
internal sealed interface SpeechDispatcherProblem {
    val message: String

    data object NotInstalled : SpeechDispatcherProblem {
        override val message: String
            get() = stringResource(MR.strings.tts_speech_dispatcher_not_installed)
    }

    /** [detail] is Speech Dispatcher's own explanation, when it gave one. */
    data class CouldNotStart(val detail: String?) : SpeechDispatcherProblem {
        override val message: String
            get() = detail?.let { stringResource(MR.strings.tts_speech_dispatcher_start_failed_because, it) }
                ?: stringResource(MR.strings.tts_speech_dispatcher_start_failed)
    }

    /** [detail] is the system's explanation of the failed connection, when it gave one. */
    data class Unreachable(val detail: String?) : SpeechDispatcherProblem {
        override val message: String
            get() = detail?.let { stringResource(MR.strings.tts_speech_dispatcher_unreachable_because, it) }
                ?: stringResource(MR.strings.tts_speech_dispatcher_unreachable)
    }

    data object NoVoices : SpeechDispatcherProblem {
        override val message: String
            get() = stringResource(MR.strings.tts_speech_dispatcher_no_voices)
    }

    data object StoppedResponding : SpeechDispatcherProblem {
        override val message: String
            get() = stringResource(MR.strings.tts_speech_dispatcher_stopped_responding)
    }

    /** Speech Dispatcher answered a command with an error; [statusLine] is its answer. */
    data class Refused(val statusLine: String) : SpeechDispatcherProblem {
        override val message: String
            get() = stringResource(MR.strings.tts_speech_dispatcher_refused, statusLine)
    }
}

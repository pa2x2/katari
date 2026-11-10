package app.katari.desktop.security.signin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.katari.desktop.presence.AppPresence
import app.katari.desktop.window.appHasFocus
import kotlinx.coroutines.flow.firstOrNull
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Reads the user's finger on [reader] for a sign-in dialog while the app has the focus and the user is there, calling
 * [onRecognized] once it recognizes one. Elsewhere the reader is let go: the desktop's lock screen and other apps read
 * it too, and a finger meant for them mustn't sign in here.
 */
@Composable
internal fun ReadsFingerprint(
    reader: FingerprintReader?,
    state: FingerprintPromptState,
    presence: AppPresence,
    onRecognized: () -> Unit,
) {
    val away by presence.isAway.collectAsState(initial = false)
    val reads = reader != null && appHasFocus() && !away && !state.tooManyTries
    LaunchedEffect(reads) {
        if (!reads) return@LaunchedEffect
        try {
            val last = reader.read().firstOrNull(state::take)
            if (last == FingerprintEvent.Recognized) onRecognized()
        } finally {
            state.stoppedReading()
        }
    }
}

/** What the dialog says about the fingerprint reader, or null while it says nothing. */
@Composable
internal fun fingerprintLine(state: FingerprintPromptState): String? = when {
    state.tooManyTries -> stringResource(MR.strings.sign_in_check_fingerprint_too_many_tries)
    else -> when (state.status) {
        FingerprintEvent.Waiting -> stringResource(MR.strings.sign_in_check_fingerprint_waiting)
        FingerprintEvent.NotRecognized -> stringResource(MR.strings.sign_in_check_fingerprint_not_recognized)
        FingerprintEvent.ReadAgain -> stringResource(MR.strings.sign_in_check_fingerprint_read_again)
        FingerprintEvent.Recognized, null -> null
    }
}

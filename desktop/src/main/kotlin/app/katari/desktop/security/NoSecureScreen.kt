package app.katari.desktop.security

import dev.zacsweers.metro.Inject
import eu.kanade.presentation.more.settings.screen.security.SecureScreen

/** Desktop systems give a window no way to keep itself out of screenshots and screen recordings. */
@Inject
class NoSecureScreen : SecureScreen {
    override val isOffered: Boolean = false
}

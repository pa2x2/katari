package eu.kanade.presentation.more.settings.screen.security

import dev.zacsweers.metro.Inject

/** Android keeps a window's content out of screenshots and the recent apps with `FLAG_SECURE`. */
@Inject
class AndroidSecureScreen : SecureScreen {
    override val isOffered: Boolean = true
}

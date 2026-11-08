package app.katari.desktop.security

import app.katari.core.platform.PlatformContext
import app.katari.desktop.security.signin.SignInPrompts
import app.katari.desktop.security.signin.SystemSignIn
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.ui.security.SettingChangeAuthentication
import tachiyomi.i18n.*
import tachiyomi.i18n.platform.stringResource

/** Asks for the user's sign-in to this computer; where the system can't check it, the change goes through unasked. */
@Inject
class DesktopSettingChangeAuthentication(
    private val signIn: SystemSignIn,
    private val prompts: SignInPrompts,
) : SettingChangeAuthentication {
    override suspend fun authenticate(context: PlatformContext, title: String): Boolean =
        !signIn.isAvailable ||
            prompts.ask(title = title, detail = null, confirmLabel = context.stringResource(MR.strings.action_apply))
}

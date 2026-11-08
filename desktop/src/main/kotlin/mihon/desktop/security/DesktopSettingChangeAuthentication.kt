package mihon.desktop.security

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.ui.security.SettingChangeAuthentication
import mihon.core.platform.PlatformContext
import mihon.desktop.security.signin.SignInPrompts
import mihon.desktop.security.signin.SystemSignIn
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

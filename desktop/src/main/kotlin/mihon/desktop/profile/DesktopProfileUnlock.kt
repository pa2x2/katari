package mihon.desktop.profile

import dev.zacsweers.metro.Inject
import mihon.core.platform.PlatformContext
import mihon.desktop.security.signin.SignInPrompts
import mihon.desktop.security.signin.SystemSignIn
import mihon.feature.profiles.ui.ProfileUnlock
import mihon.presentation.kit.feedback.AppMessages
import tachiyomi.i18n.*
import tachiyomi.i18n.platform.stringResource

/**
 * Unlocks with the user's sign-in to this computer, which the system checks (see [SystemSignIn]). Where it can't, a
 * locked profile stays shut and a message says why.
 */
@Inject
class DesktopProfileUnlock(
    private val signIn: SystemSignIn,
    private val prompts: SignInPrompts,
    private val messages: AppMessages,
) : ProfileUnlock {
    override fun isAvailable(context: PlatformContext): Boolean = signIn.isAvailable

    override suspend fun unlock(context: PlatformContext, profileNames: List<String>): Boolean {
        if (profileNames.isEmpty()) return true
        return ask(
            context = context,
            names = profileNames.joinToString(),
            title = context.stringResource(MR.strings.unlock_app_title, profileNames.joinToString()),
            detail = null,
        )
    }

    override suspend fun unlockToMoveEntries(context: PlatformContext, profileName: String): Boolean = ask(
        context = context,
        names = profileName,
        title = context.stringResource(MR.strings.move_entries_auth_title),
        detail = context.stringResource(MR.strings.move_entries_auth_subtitle, profileName),
    )

    private suspend fun ask(context: PlatformContext, names: String, title: String, detail: String?): Boolean {
        if (!signIn.isAvailable) {
            messages.show(context.stringResource(MR.strings.profile_locked_cant_check, names))
            return false
        }
        return prompts.ask(title, detail, confirmLabel = context.stringResource(MR.strings.action_unlock))
    }
}

package app.katari.desktop.profile

import app.katari.core.platform.PlatformContext
import app.katari.desktop.security.signin.SignInPrompts
import app.katari.desktop.security.signin.SystemSignIn
import app.katari.presentation.kit.feedback.AppMessages
import app.katari.profiles.ui.ProfileLockWording
import app.katari.profiles.ui.ProfileUnlock
import dev.zacsweers.metro.Inject
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

    override val lockSettingsWording = ProfileLockWording(
        requireUnlock = MR.strings.lock_with_sign_in_summary,
        lockWhenIdle = MR.strings.lock_when_idle_desktop_summary,
        unavailable = MR.strings.profile_lock_unavailable_summary,
    )

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

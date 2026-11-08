package app.katari.desktop.onboarding

import androidx.compose.runtime.Composable
import dev.zacsweers.metro.Inject
import eu.kanade.presentation.more.onboarding.OnboardingPermissions
import eu.kanade.presentation.more.onboarding.PermissionRequest

/**
 * Desktop systems grant the app what Android asks permission for, such as installing extensions, posting
 * notifications and working in the background, so the first-run guide has no permission to ask for.
 */
@Inject
class NoOnboardingPermissions : OnboardingPermissions {
    override val isOffered: Boolean = false

    @Composable
    override fun rememberRequests(): List<PermissionRequest> = emptyList()
}

package eu.kanade.presentation.more.onboarding

import androidx.compose.runtime.Composable
import dev.icerock.moko.resources.StringResource

/**
 * The system permissions the first-run guide asks the user to grant, such as Android's to install extensions, post
 * notifications and run without battery optimisation.
 */
interface OnboardingPermissions {
    /** False where the platform has no permission to ask for; the guide then lists none. */
    val isOffered: Boolean

    /** The permissions in the order the guide lists them, as they stand now. Only called while [isOffered]. */
    @Composable
    fun rememberRequests(): List<PermissionRequest>
}

data class PermissionRequest(
    val title: StringResource,
    val description: StringResource,
    val granted: Boolean,
    /** Opens the system's prompt or screen that grants the permission. */
    val request: () -> Unit,
)

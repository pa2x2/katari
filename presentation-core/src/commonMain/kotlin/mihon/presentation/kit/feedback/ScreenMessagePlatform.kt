package mihon.presentation.kit.feedback

import androidx.compose.runtime.Composable
import dev.icerock.moko.resources.StringResource

/** What [ScreenMessages] needs from the platform: strings, and a way to show a message with no screen to show it on. */
internal interface ScreenMessagePlatform {
    fun string(resource: StringResource): String

    fun showOutsideScreen(message: String, long: Boolean)
}

@Composable
internal expect fun rememberScreenMessagePlatform(): ScreenMessagePlatform

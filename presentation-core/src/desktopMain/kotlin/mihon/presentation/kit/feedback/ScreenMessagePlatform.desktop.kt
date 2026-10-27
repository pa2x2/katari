package mihon.presentation.kit.feedback

import androidx.compose.runtime.Composable
import dev.icerock.moko.resources.StringResource
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.localized.stringResource

@Composable
internal actual fun rememberScreenMessagePlatform(): ScreenMessagePlatform = DesktopScreenMessagePlatform

// Every desktop window hosts screen messages, so one that reaches here was shown from outside any window
private object DesktopScreenMessagePlatform : ScreenMessagePlatform {
    override fun string(resource: StringResource): String = stringResource(resource)

    override fun showOutsideScreen(message: String, long: Boolean) {
        logcat(LogPriority.WARN) { "Screen message outside any window: $message" }
    }
}

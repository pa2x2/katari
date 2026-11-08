package app.katari.presentation.kit.feedback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.localized.stringResource

@Composable
internal actual fun rememberScreenMessagePlatform(): ScreenMessagePlatform {
    val appMessages = LocalAppMessages.current
    return remember(appMessages) { AppMessageScreenMessagePlatform(appMessages) }
}

private class AppMessageScreenMessagePlatform(private val appMessages: AppMessages) : ScreenMessagePlatform {
    override fun string(resource: StringResource): String = stringResource(resource)

    override fun showOutsideScreen(message: String, long: Boolean) {
        appMessages.show(message, long)
    }
}

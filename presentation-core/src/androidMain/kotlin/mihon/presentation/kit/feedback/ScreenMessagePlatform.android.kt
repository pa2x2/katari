package mihon.presentation.kit.feedback

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.context.stringResource

@Composable
internal actual fun rememberScreenMessagePlatform(): ScreenMessagePlatform {
    val context = LocalContext.current
    return remember(context) { ToastScreenMessagePlatform(context) }
}

private class ToastScreenMessagePlatform(private val context: Context) : ScreenMessagePlatform {
    override fun string(resource: StringResource): String = context.stringResource(resource)

    override fun showOutsideScreen(message: String, long: Boolean) {
        Toast.makeText(context.applicationContext, message, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
    }
}

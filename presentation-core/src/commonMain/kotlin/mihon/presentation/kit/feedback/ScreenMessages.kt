package mihon.presentation.kit.feedback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import dev.icerock.moko.resources.StringResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Shows a short message about the screen the user is on, such as "Cache cleared", as a snackbar on that screen. A new
 * message replaces the one showing, as toasts did. A message that comes from no screen, such as a worker's, goes
 * through [AppMessages], or is a toast where Android code already shows one.
 *
 * Each navigation screen gets its own messages from [ScreenMessagesHost], and the first kit Scaffold inside it shows
 * them, so a screen can take them anywhere in its content, above its Scaffold too. A screen without a Scaffold, such as
 * one on a sheet, passes its messages to the screen or sheet around it; with none around, Android shows them as toasts.
 */
@Stable
class ScreenMessages internal constructor(
    private val platform: ScreenMessagePlatform,
    internal val host: SnackbarHostState?,
    // The scope of the screen that owns the messages, not of the caller, so a message from a dialog that closes right
    // after showing it stays up.
    private val scope: CoroutineScope?,
    private val outer: ScreenMessages?,
) {
    // Who shows this host's snackbars: the first Scaffold of the screen, or the Sheet that owns them.
    private var shownBy by mutableStateOf<Any?>(null)

    internal val shown: Boolean get() = shownBy != null

    internal fun isShownBy(owner: Any): Boolean = shownBy === owner

    fun show(message: String, long: Boolean = false) {
        when {
            host != null && scope != null && shown -> scope.launch {
                host.currentSnackbarData?.dismiss()
                host.showSnackbar(message, duration = if (long) SnackbarDuration.Long else SnackbarDuration.Short)
            }
            outer != null -> outer.show(message, long)
            else -> platform.showOutsideScreen(message, long)
        }
    }

    fun show(message: StringResource, long: Boolean = false) = show(platform.string(message), long)

    /** Lets [owner] show this host's snackbars unless another owner already does. */
    internal fun claim(owner: Any) {
        if (host != null && shownBy == null) shownBy = owner
    }

    internal fun release(owner: Any) {
        if (shownBy === owner) shownBy = null
    }
}

/** The messages of the screen or sheet the caller is on. */
@Composable
fun rememberScreenMessages(): ScreenMessages {
    val provided = LocalScreenMessages.current
    val platform = rememberScreenMessagePlatform()
    return provided ?: remember(platform) { ScreenMessages(platform, host = null, scope = null, outer = null) }
}

/** Gives [content], one navigation screen, its own [ScreenMessages]. */
@Composable
fun ScreenMessagesHost(content: @Composable () -> Unit) {
    val messages = rememberScreenMessagesFor(remember { SnackbarHostState() })
    CompositionLocalProvider(LocalScreenMessages provides messages, content = content)
}

/** Messages shown through [host], drawn by whoever [claimScreenMessages] picks. */
@Composable
internal fun rememberScreenMessagesFor(host: SnackbarHostState): ScreenMessages {
    val platform = rememberScreenMessagePlatform()
    val outer = LocalScreenMessages.current
    val scope = rememberCoroutineScope()
    val messages = remember(platform, host, scope, outer) { ScreenMessages(platform, host, scope, outer) }
    val appMessageTargets = LocalAppMessageTargets.current
    if (appMessageTargets != null) {
        DisposableEffect(appMessageTargets, messages) {
            appMessageTargets.add(messages)
            onDispose { appMessageTargets.remove(messages) }
        }
    }
    return messages
}

/**
 * Claims [messages] for the caller, typically the screen's first Scaffold, once no one else shows them; returns their
 * host while the caller is the one that should draw it. A later claimant takes over when the earlier one leaves, such
 * as the next tab's Scaffold.
 */
@Composable
internal fun claimScreenMessages(messages: ScreenMessages? = LocalScreenMessages.current): SnackbarHostState? {
    messages ?: return null
    val owner = remember { Any() }
    LaunchedEffect(messages) {
        snapshotFlow { messages.shown }.collect { shown -> if (!shown) messages.claim(owner) }
    }
    DisposableEffect(messages) { onDispose { messages.release(owner) } }
    return messages.host.takeIf { messages.isShownBy(owner) }
}

internal val LocalScreenMessages: ProvidableCompositionLocal<ScreenMessages?> = staticCompositionLocalOf { null }

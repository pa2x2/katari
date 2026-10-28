package mihon.presentation.kit.feedback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

/**
 * Short messages that come from no screen, such as how a backup ended or why a link didn't open, posted from anywhere
 * in the app and shown on the screen the user is on as one of its own [ScreenMessages]. The window's host shows them
 * through [AppMessagesHost]. A message posted while no screen shows messages, such as before the home screen opens,
 * waits for one; past [WAITING_CAPACITY] waiting, the oldest is dropped.
 */
@Inject
@SingleIn(AppScope::class)
class AppMessages {
    private val waiting = Channel<AppMessage>(WAITING_CAPACITY, BufferOverflow.DROP_OLDEST)

    fun show(message: String, long: Boolean = false) {
        waiting.trySend(AppMessage(message, long))
    }

    internal suspend fun next(): AppMessage = waiting.receive()

    private companion object {
        const val WAITING_CAPACITY = 8
    }
}

internal class AppMessage(val text: String, val long: Boolean)

/** Shows [messages] on the screen or sheet in [content] that opened last and shows messages. */
@Composable
fun AppMessagesHost(messages: AppMessages, content: @Composable () -> Unit) {
    val targets = remember { AppMessageTargets() }
    LaunchedEffect(messages, targets) {
        while (true) {
            val message = messages.next()
            snapshotFlow { targets.current() }.filterNotNull().first().show(message.text, message.long)
        }
    }
    CompositionLocalProvider(LocalAppMessageTargets provides targets, content = content)
}

/**
 * The screen and sheet messages composed under an [AppMessagesHost], in the order they appeared. A screen pushed or
 * returned to, or a sheet opened, is composed after the one it covers, so the last of them that shows its messages is
 * the one the user is looking at.
 */
internal class AppMessageTargets {
    private val entries = mutableStateListOf<ScreenMessages>()

    fun add(messages: ScreenMessages) {
        entries += messages
    }

    fun remove(messages: ScreenMessages) {
        entries -= messages
    }

    fun current(): ScreenMessages? = entries.lastOrNull { it.shown }
}

internal val LocalAppMessageTargets: ProvidableCompositionLocal<AppMessageTargets?> = staticCompositionLocalOf { null }

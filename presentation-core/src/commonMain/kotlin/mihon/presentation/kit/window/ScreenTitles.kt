package mihon.presentation.kit.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The titles of the screens shown in one window, as their app bars name them, for a window whose title names what is
 * open. [current] is the newest screen's: a screen pushed or revealed is composed after the one leaving, which may still
 * be shown while the transition runs. A platform whose windows show no title of the app's provides none.
 */
@Stable
class ScreenTitles {
    private val titles = mutableStateListOf<ScreenTitle>()

    /** Null while no shown screen names itself. */
    val current: String?
        get() = titles.lastOrNull { it.title != null }?.title

    internal fun add(title: ScreenTitle) {
        titles += title
    }

    internal fun remove(title: ScreenTitle) {
        titles -= title
    }
}

internal class ScreenTitle {
    var title by mutableStateOf<String?>(null)
}

val LocalScreenTitles = staticCompositionLocalOf<ScreenTitles?> { null }

/** Names the screen composing this [title] to the window's [ScreenTitles] while it is composed. */
@Composable
internal fun ReportScreenTitle(title: String?) {
    val titles = LocalScreenTitles.current ?: return
    val entry = remember(titles) { ScreenTitle() }
    SideEffect { entry.title = title }
    DisposableEffect(entry) {
        titles.add(entry)
        onDispose { titles.remove(entry) }
    }
}

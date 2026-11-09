package app.katari.translation.provider.server.setup

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

/**
 * A provider's setup as a screen of its own, which both platforms show: on a window's navigator, which it leaves when
 * the user is done, or alone in Android's setup activity, which closes instead (see [Alone]).
 */
abstract class ProviderSetupScreen : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    protected abstract fun Setup(onClose: () -> Unit)

    @Composable
    final override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        Setup(onClose = { navigator.pop() })
    }

    /** Shows the setup outside a navigator, as the only content of its host, which [onClose] closes. */
    @Composable
    fun Alone(onClose: () -> Unit) {
        Setup(onClose)
    }
}

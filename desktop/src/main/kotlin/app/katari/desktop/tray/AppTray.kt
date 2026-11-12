package app.katari.desktop.tray

import app.katari.desktop.application.DesktopAppVariant
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import tachiyomi.i18n.*
import tachiyomi.i18n.localized.stringResource

/**
 * The app's icon in the desktop's tray ([StatusNotifierItem]), there while [TrayPreferences.keepRunningInTray] is on,
 * from [start] until [close]. Its menu opens the window and quits the app; what it asks for arrives in [requests].
 */
@SingleIn(AppScope::class)
@Inject
class AppTray(private val preferences: TrayPreferences) : AutoCloseable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val asked = Channel<TrayRequest>(Channel.UNLIMITED)

    private val item = StatusNotifierItem(
        id = DesktopAppVariant.current.applicationId,
        title = DesktopAppVariant.current.displayName,
        menu = TrayMenu(
            openLabel = stringResource(PlatformStrings.tray_open_app, DesktopAppVariant.current.displayName),
            quitLabel = stringResource(MR.strings.action_quit),
        ),
        onRequest = { asked.trySend(it) },
    )

    /** Whether the desktop runs a tray the icon can show in. */
    val hostAvailable: StateFlow<Boolean> = item.hostAvailable

    /** Whether the icon shows in a tray now, which the app can be brought back and quit from. */
    val isShown: StateFlow<Boolean> = item.isShown

    /** What the tray asks the app for, each once, to collect on the window's thread. */
    internal val requests: Flow<TrayRequest> = asked.receiveAsFlow()

    fun start() {
        item.start()
        preferences.keepRunningInTray.changes().onEach(item::show).launchIn(scope)
    }

    override fun close() {
        scope.cancel()
        item.close()
    }
}

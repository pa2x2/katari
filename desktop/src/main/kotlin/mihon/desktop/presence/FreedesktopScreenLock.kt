package mihon.desktop.presence

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import mihon.desktop.dbus.Gdbus

/**
 * Linux: whether the session's screen is locked or blanked, as the desktop tells it: logind's `LockedHint` of the
 * app's session, which GNOME's and KDE's lock screens set, or the `org.freedesktop.ScreenSaver` service's active state,
 * which KDE, Xfce and LXQt keep. A locker that tells neither, such as swaylock, isn't seen.
 */
class FreedesktopScreenLock(scope: CoroutineScope) : ScreenLock {
    override val isLocked: Flow<Boolean> =
        combine(logindLockedHint(scope), screenSaverActive(scope)) { locked, active ->
            locked || active
        }

    private fun logindLockedHint(scope: CoroutineScope): Flow<Boolean> = flow {
        // gdbus prints the session as `(objectpath '/org/freedesktop/login1/session/_32',)`
        val session = Logind.call(GET_SESSION, "auto", timeoutSeconds = READ_TIMEOUT_SECONDS)
            ?.let { OBJECT_PATH.find(it)?.groupValues?.get(1) }
        if (session == null) {
            emit(false)
            return@flow
        }
        val sessionObject = Gdbus(Gdbus.Bus.System, LOGIND, session)
        emitAll(
            sessionObject.watch(scope, PROPERTIES_CHANGED) {
                // gdbus prints the property as `(<true>,)`
                sessionObject.call(PROPERTY_GET, SESSION_INTERFACE, "LockedHint", timeoutSeconds = READ_TIMEOUT_SECONDS)
                    ?.contains("true") == true
            },
        )
    }.flowOn(Dispatchers.IO)

    private fun screenSaverActive(scope: CoroutineScope): Flow<Boolean> =
        ScreenSaver.watch(scope, ACTIVE_CHANGED) {
            // gdbus prints the answer as `(true,)`
            ScreenSaver.call(GET_ACTIVE, timeoutSeconds = READ_TIMEOUT_SECONDS)?.contains("true") == true
        }

    private object Logind : Gdbus(Bus.System, LOGIND, "/org/freedesktop/login1")

    private object ScreenSaver : Gdbus(Bus.Session, SCREEN_SAVER, "/org/freedesktop/ScreenSaver")

    private companion object {
        const val LOGIND = "org.freedesktop.login1"
        const val SESSION_INTERFACE = "org.freedesktop.login1.Session"
        const val GET_SESSION = "org.freedesktop.login1.Manager.GetSession"
        const val PROPERTY_GET = "org.freedesktop.DBus.Properties.Get"
        const val PROPERTIES_CHANGED = "org.freedesktop.DBus.Properties.PropertiesChanged"
        const val SCREEN_SAVER = "org.freedesktop.ScreenSaver"
        const val GET_ACTIVE = "org.freedesktop.ScreenSaver.GetActive"
        const val ACTIVE_CHANGED = "org.freedesktop.ScreenSaver.ActiveChanged"
        const val READ_TIMEOUT_SECONDS = 2
        val OBJECT_PATH = Regex("""'(/[^']+)'""")
    }
}

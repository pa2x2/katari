package mihon.desktop.profile

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mihon.core.metro.AppCoroutineScope
import mihon.desktop.player.WindowAnimePlayer
import mihon.desktop.presence.AppPresence
import mihon.desktop.security.signin.SystemSignIn
import kotlin.time.Duration.Companion.minutes

/**
 * Locks the open profile again once the user has been away from the app ([AppPresence]) for its "Lock when idle" time,
 * as Android locks the app after that long in the background: "Always" as soon as they leave, "Never" not at all. It
 * locks while the user is still away, so the window, its preview and the desktop's media controls stop showing what
 * was open: the player closes and the window's profile gate covers the profile. Only where the system can check the
 * sign-in, without which the profile couldn't open again.
 */
@SingleIn(AppScope::class)
@Inject
class ProfileRelock(
    private val presence: AppPresence,
    private val lock: ActiveProfileLock,
    private val security: SecurityPreferences,
    private val signIn: SystemSignIn,
    private val player: WindowAnimePlayer,
    @AppCoroutineScope private val scope: CoroutineScope,
) {
    fun start() {
        scope.launch {
            // A return before the delay ends, or a new absence, starts over
            presence.isAway.collectLatest { away ->
                if (!away || lock.isHeld.value || !signIn.isAvailable || !security.useAuthenticator.get()) {
                    return@collectLatest
                }
                when (val minutes = security.lockAppAfter.get()) {
                    SecurityPreferences.LOCK_NEVER -> return@collectLatest
                    SecurityPreferences.LOCK_ALWAYS -> Unit
                    else -> delay(minutes.minutes)
                }
                withContext(Dispatchers.Main) { player.visit?.let(player::close) }
                lock.hold()
            }
        }
    }
}

package app.katari.profiles.core

import android.content.Intent
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.util.concurrent.atomic.AtomicReference

/**
 * The intent the app was opened with, kept while a profile is picked or unlocked and handled once one is open. Kept as
 * the intent itself because it is consumed in the same process, and an `intent:` URI keeps only primitive extras.
 */
@Inject
@SingleIn(AppScope::class)
class ProfilePendingIntent {
    private val intent = AtomicReference<Intent?>(null)

    fun store(intent: Intent?) {
        this.intent.set(intent)
    }

    fun consume(): Intent? = intent.getAndSet(null)
}

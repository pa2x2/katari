package app.katari.android.time

import android.os.SystemClock
import app.katari.ui.automaticcheck.SleepCountingClock
import dev.zacsweers.metro.Inject

@Inject
class ElapsedRealtimeClock : SleepCountingClock {
    override fun elapsedMillis(): Long = SystemClock.elapsedRealtime()
}

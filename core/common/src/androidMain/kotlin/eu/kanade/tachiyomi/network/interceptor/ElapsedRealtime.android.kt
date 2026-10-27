package eu.kanade.tachiyomi.network.interceptor

import android.os.SystemClock

// Counts deep sleep too, so a window that elapsed while the device slept doesn't hold back the next request.
internal actual fun elapsedRealtimeMillis(): Long = SystemClock.elapsedRealtime()

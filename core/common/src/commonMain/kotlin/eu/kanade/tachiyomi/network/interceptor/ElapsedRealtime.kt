package eu.kanade.tachiyomi.network.interceptor

/** Milliseconds on a monotonic clock, for measuring rate-limit windows. */
internal expect fun elapsedRealtimeMillis(): Long

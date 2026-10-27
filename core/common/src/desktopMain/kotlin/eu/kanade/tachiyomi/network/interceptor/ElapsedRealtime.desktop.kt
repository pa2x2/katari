package eu.kanade.tachiyomi.network.interceptor

internal actual fun elapsedRealtimeMillis(): Long = System.nanoTime() / 1_000_000

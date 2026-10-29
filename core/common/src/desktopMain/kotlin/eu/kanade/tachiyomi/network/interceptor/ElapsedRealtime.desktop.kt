// Extensions compiled against the Android jar link against Android's file class.
@file:JvmName("ElapsedRealtime_androidKt")

package eu.kanade.tachiyomi.network.interceptor

internal actual fun elapsedRealtimeMillis(): Long = System.nanoTime() / 1_000_000

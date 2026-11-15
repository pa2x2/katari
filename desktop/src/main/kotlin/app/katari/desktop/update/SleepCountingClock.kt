package app.katari.desktop.update

import app.katari.core.common.platform.DesktopOperatingSystem
import com.sun.jna.Function

/**
 * Milliseconds since the system booted, counting the time it was suspended. The JVM's own monotonic clock,
 * [System.nanoTime], reads Linux's `CLOCK_MONOTONIC`, which stops while the system is suspended.
 */
internal fun sleepCountingMillis(operatingSystem: DesktopOperatingSystem): Long = when (operatingSystem) {
    DesktopOperatingSystem.LINUX -> {
        // struct timespec: seconds and nanoseconds, a 64-bit long each on the 64-bit systems the app runs on
        val timespec = LongArray(2)
        val result = linuxClockGettime.invokeInt(arrayOf(LINUX_CLOCK_BOOTTIME, timespec))
        check(result == 0) { "clock_gettime failed" }
        timespec[0] * MILLIS_PER_SECOND + timespec[1] / NANOS_PER_MILLI
    }
}

private val linuxClockGettime: Function by lazy { Function.getFunction("c", "clock_gettime") }

private const val LINUX_CLOCK_BOOTTIME = 7
private const val MILLIS_PER_SECOND = 1_000L
private const val NANOS_PER_MILLI = 1_000_000L

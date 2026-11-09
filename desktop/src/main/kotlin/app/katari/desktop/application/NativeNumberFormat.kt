package app.katari.desktop.application

import app.katari.core.common.platform.DesktopOperatingSystem
import com.sun.jna.Library
import com.sun.jna.Native

/**
 * Sets the C library's number format back to C's for the whole process, before any native library loads and before
 * other threads start, as setting it is not thread-safe.
 *
 * The Java launcher takes every locale category from the environment, but native libraries read and write numbers
 * through the C library: libmpv refuses to start unless numbers have a decimal point, and under a decimal comma ONNX
 * Runtime builds a different graph for PaddleOCR's line detector, which then finds no text. The JVM formats its own
 * numbers, so nothing in the process needs the user's format.
 */
internal fun useCNumberFormat(operatingSystem: DesktopOperatingSystem) {
    when (operatingSystem) {
        DesktopOperatingSystem.LINUX -> LinuxLocale.INSTANCE.setlocale(LINUX_LC_NUMERIC, "C")
    }
}

private interface LinuxLocale : Library {
    fun setlocale(category: Int, locale: String?): String?

    companion object {
        val INSTANCE: LinuxLocale = Native.load("c", LinuxLocale::class.java)
    }
}

private const val LINUX_LC_NUMERIC = 1

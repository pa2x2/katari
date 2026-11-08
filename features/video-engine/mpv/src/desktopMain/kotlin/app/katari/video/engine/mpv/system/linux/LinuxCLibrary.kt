package app.katari.video.engine.mpv.system.linux

import com.sun.jna.Library
import com.sun.jna.Native

/**
 * The Linux C library's calls made around libmpv: its number locale, and the device it decodes on. The constants are
 * Linux's.
 */
internal interface LinuxCLibrary : Library {
    fun setlocale(category: Int, locale: String?): String?

    fun open(path: String, flags: Int): Int

    fun close(fd: Int): Int

    companion object {
        val INSTANCE: LinuxCLibrary by lazy { Native.load("c", LinuxCLibrary::class.java) }

        const val LC_NUMERIC = 1
        const val O_RDWR = 2
        const val O_CLOEXEC = 0x80000
    }
}

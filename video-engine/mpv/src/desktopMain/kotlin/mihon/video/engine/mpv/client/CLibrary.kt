package mihon.video.engine.mpv.client

import com.sun.jna.Library
import com.sun.jna.Native

/** The C library calls made around libmpv: its number locale, and the device it decodes on. */
internal interface CLibrary : Library {
    fun setlocale(category: Int, locale: String?): String?

    fun open(path: String, flags: Int): Int

    fun close(fd: Int): Int

    companion object {
        val INSTANCE: CLibrary by lazy { Native.load("c", CLibrary::class.java) }

        const val LC_NUMERIC = 1
        const val O_RDWR = 2
        const val O_CLOEXEC = 0x80000
    }
}

package mihon.video.engine.mpv.system

import com.sun.jna.Pointer
import mihon.core.common.platform.DesktopOperatingSystem
import mihon.video.engine.mpv.client.LibMpv
import mihon.video.engine.mpv.render.MemoryFrames
import mihon.video.engine.mpv.system.linux.LinuxMpvSystem

/** What libmpv and its rendering need of the operating system the app runs on. */
internal interface MpvSystem {
    /**
     * Readies the process for libmpv, once it loads: libmpv refuses to create an instance unless the C library's
     * numbers are formatted with a dot.
     */
    fun prepareProcess()

    /** The best of the system's renderers that starts for the mpv instance [handle], or null when none does. */
    fun createFrames(mpv: LibMpv, handle: Pointer): MemoryFrames?
}

internal val mpvSystem: MpvSystem by lazy {
    when (DesktopOperatingSystem.current()) {
        DesktopOperatingSystem.LINUX -> LinuxMpvSystem
    }
}

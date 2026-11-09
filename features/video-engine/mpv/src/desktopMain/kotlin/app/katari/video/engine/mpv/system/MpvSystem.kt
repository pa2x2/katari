package app.katari.video.engine.mpv.system

import app.katari.core.common.platform.DesktopOperatingSystem
import app.katari.video.engine.mpv.client.LibMpv
import app.katari.video.engine.mpv.render.MemoryFrames
import app.katari.video.engine.mpv.system.linux.LinuxMpvSystem
import com.sun.jna.Pointer

/** What libmpv and its rendering need of the operating system the app runs on. */
internal interface MpvSystem {
    /** The best of the system's renderers that starts for the mpv instance [handle], or null when none does. */
    fun createFrames(mpv: LibMpv, handle: Pointer): MemoryFrames?
}

internal val mpvSystem: MpvSystem by lazy {
    when (DesktopOperatingSystem.current()) {
        DesktopOperatingSystem.LINUX -> LinuxMpvSystem
    }
}

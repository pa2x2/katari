package eu.kanade.tachiyomi.util.system

import android.content.Context
import java.lang.management.ManagementFactory
import com.sun.management.OperatingSystemMXBean as PlatformOperatingSystem

/** Android's device checks. Desktop is no phone maker's build, so only the memory check asks the machine. */
object DeviceUtil {

    val isMiui: Boolean = false

    val miuiMajorVersion: Int? = null

    fun isMiuiOptimizationDisabled(): Boolean = false

    val isSamsung: Boolean = false

    val oneUiVersion: Double? = null

    /** The packages Android may wrongly resolve as browsers; desktop opens links through no Android resolver. */
    val invalidDefaultBrowsers = emptyList<String>()

    /** Less than 3 GB of memory, the threshold Android uses, given how heavy image processing can be. */
    fun isLowRamDevice(context: Context): Boolean {
        val system = ManagementFactory.getOperatingSystemMXBean() as? PlatformOperatingSystem ?: return false
        return system.totalMemorySize < 3L * 1024 * 1024 * 1024
    }
}

package mihon.video.engine.mpv.loading

import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Process
import java.io.File

/**
 * The ABI an app's native code runs as, read from where Android extracted its libraries (`…/lib/arm64`). For Katari
 * that is the ABI of the running process, which can differ from the device's first ABI: a 32-bit install on a 64-bit
 * device, or an ARM build translated on an x86 device.
 */
internal fun ApplicationInfo.nativeAbi(): String? = abiOfInstructionSet(File(nativeLibraryDir ?: return null).name)

/** Katari's own process ABI; [nativeAbi] when Katari has native libraries, which every build does. */
internal fun ApplicationInfo.processAbi(): String = nativeAbi()
    ?: if (Process.is64Bit()) Build.SUPPORTED_64_BIT_ABIS.first() else Build.SUPPORTED_32_BIT_ABIS.first()

private fun abiOfInstructionSet(directory: String): String? = when (directory) {
    "arm64" -> "arm64-v8a"
    "arm" -> "armeabi-v7a"
    "x86_64" -> "x86_64"
    "x86" -> "x86"
    else -> null
}

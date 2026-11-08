package app.katari.tools.extensioncompatibility.run

import java.lang.management.ManagementFactory
import java.lang.management.MemoryType

/**
 * The heap the run's extensions keep alive, measured after a full collection so it doesn't depend on when the collector
 * last ran. One measurement at a time, since parallel installs would otherwise collect over each other.
 */
internal object HeapMeter {
    private const val MB = 1024L * 1024L

    @Synchronized
    fun liveHeapMb(): Long {
        System.gc()
        return ManagementFactory.getMemoryMXBean().heapMemoryUsage.used / MB
    }

    fun metaspaceMb(): Long = ManagementFactory.getMemoryPoolMXBeans()
        .filter { it.type == MemoryType.NON_HEAP && it.name == "Metaspace" }
        .sumOf { it.usage.used } / MB

    fun maxHeapMb(): Long = Runtime.getRuntime().maxMemory() / MB
}

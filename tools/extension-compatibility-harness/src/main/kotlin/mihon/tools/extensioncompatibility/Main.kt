package mihon.tools.extensioncompatibility

import kotlinx.coroutines.runBlocking
import mihon.tools.extensioncompatibility.environment.SmokeEnvironment
import mihon.tools.extensioncompatibility.options.SmokeOptions
import mihon.tools.extensioncompatibility.options.UsageException
import mihon.tools.extensioncompatibility.options.readStoreList
import mihon.tools.extensioncompatibility.report.ReportFiles
import mihon.tools.extensioncompatibility.run.SmokeRun
import kotlin.system.exitProcess

/**
 * Exits with 0 once the report is written, whatever the extensions did, and with 2 for arguments that don't make a run.
 * It exits rather than returns, since the host's main looper and the extensions' clients keep threads running.
 */
fun main(args: Array<String>) {
    val options = try {
        SmokeOptions.parse(args)
    } catch (e: UsageException) {
        e.message?.let(System.err::println)
        System.err.println(SmokeOptions.USAGE)
        exitProcess(if (e.message == null) 0 else 2)
    }
    val storeUrls = readStoreList(options.storeList)
    runBlocking {
        SmokeEnvironment.start(options.keepData).use { environment ->
            println("Run folder: ${environment.root}${if (options.keepData) " (kept)" else ""}")
            val results = SmokeRun(options, environment.host).run(storeUrls)
            val written = ReportFiles(options.reportDirectory).write(results, options.compareWith)
            println("Report: ${written.markdown}")
            println("Results: ${written.json}")
        }
    }
    exitProcess(0)
}

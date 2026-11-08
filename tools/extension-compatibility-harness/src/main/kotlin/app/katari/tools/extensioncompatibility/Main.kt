package app.katari.tools.extensioncompatibility

import app.katari.tools.extensioncompatibility.environment.SmokeEnvironment
import app.katari.tools.extensioncompatibility.options.SmokeOptions
import app.katari.tools.extensioncompatibility.options.UsageException
import app.katari.tools.extensioncompatibility.options.readStoreList
import app.katari.tools.extensioncompatibility.report.ReportFiles
import app.katari.tools.extensioncompatibility.run.SmokeRun
import kotlinx.coroutines.runBlocking
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

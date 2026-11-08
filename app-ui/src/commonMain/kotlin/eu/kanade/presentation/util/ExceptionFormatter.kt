package eu.kanade.presentation.util

import app.katari.core.platform.PlatformContext
import app.katari.core.platform.network.NetworkConnectivity
import app.katari.entry.interactions.catalogue.EntryCatalogueNoResultsException
import app.katari.script.runtime.ScriptException
import eu.kanade.tachiyomi.network.HttpException
import eu.kanade.tachiyomi.source.failure.ExtensionCodeException
import tachiyomi.domain.source.model.SourceNotInstalledException
import tachiyomi.i18n.*
import tachiyomi.i18n.platform.stringResource
import java.net.UnknownHostException

/**
 * The shared words for [error]: what an HTTP status, a lost connection, a failing source script or extension code that
 * couldn't run means, and for any other error its type and message.
 */
internal fun PlatformContext.formattedSourceError(error: Throwable, network: NetworkConnectivity): String {
    when (error) {
        is HttpException -> return stringResource(MR.strings.exception_http, error.code)
        is UnknownHostException -> {
            return if (!network.current().hasNetwork) {
                stringResource(MR.strings.exception_offline)
            } else {
                stringResource(MR.strings.exception_unknown_host, error.message ?: "")
            }
        }

        is EntryCatalogueNoResultsException -> return stringResource(MR.strings.no_results_found)
        is SourceNotInstalledException -> return stringResource(MR.strings.loader_not_implemented_error)
        // A script source's own error is written for users, as its app shows it
        is ScriptException.Thrown -> return error.message?.takeIf { it.isNotBlank() } ?: error.jsName
        is ScriptException.Stalled -> return stringResource(MR.strings.exception_source_stopped)
        is ScriptException.TimedOut -> return stringResource(MR.strings.anime_problem_timed_out)
        is ScriptException.OutOfMemory -> return stringResource(MR.strings.exception_source_out_of_memory)
        is ScriptException.LoadFailed -> return stringResource(MR.strings.exception_source_load_failed)
        is ExtensionCodeException -> return stringResource(MR.strings.exception_source_code_failed, error.errorType)
    }
    return when (val className = error::class.simpleName) {
        "Exception", "IOException" -> error.message ?: className
        else -> "$className: ${error.message}"
    }
}

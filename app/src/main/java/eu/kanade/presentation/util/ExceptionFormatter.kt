package eu.kanade.presentation.util

import android.content.Context
import eu.kanade.tachiyomi.network.HttpException
import eu.kanade.tachiyomi.util.system.isOnline
import mihon.entry.interactions.catalogue.EntryCatalogueNoResultsException
import mihon.script.runtime.ScriptException
import tachiyomi.domain.source.model.SourceNotInstalledException
import tachiyomi.i18n.*
import tachiyomi.i18n.context.stringResource
import java.net.UnknownHostException

context(context: Context)
val Throwable.formattedMessage: String
    get() {
        when (this) {
            is HttpException -> return context.stringResource(MR.strings.exception_http, code)
            is UnknownHostException -> {
                return if (!context.isOnline()) {
                    context.stringResource(MR.strings.exception_offline)
                } else {
                    context.stringResource(MR.strings.exception_unknown_host, message ?: "")
                }
            }

            is EntryCatalogueNoResultsException -> return context.stringResource(MR.strings.no_results_found)
            is SourceNotInstalledException -> return context.stringResource(MR.strings.loader_not_implemented_error)
            // A script source's own error is written for users, as its app shows it
            is ScriptException.Thrown -> return message?.takeIf { it.isNotBlank() } ?: jsName
            is ScriptException.Stalled -> return context.stringResource(MR.strings.exception_source_stopped)
            is ScriptException.TimedOut -> return context.stringResource(MR.strings.anime_problem_timed_out)
            is ScriptException.OutOfMemory -> return context.stringResource(MR.strings.exception_source_out_of_memory)
            is ScriptException.LoadFailed -> return context.stringResource(MR.strings.exception_source_load_failed)
        }
        return when (val className = this::class.simpleName) {
            "Exception", "IOException" -> message ?: className
            else -> "$className: $message"
        }
    }

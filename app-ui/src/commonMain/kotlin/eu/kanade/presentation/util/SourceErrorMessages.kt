package eu.kanade.presentation.util

import app.katari.core.platform.PlatformContext
import app.katari.core.platform.network.NetworkConnectivity
import dev.zacsweers.metro.Inject

/**
 * Words an error that reaching a source raised, such as a failed refresh's, for the user: what an HTTP status, a lost
 * connection or a failing source script means rather than the exception's own text. A platform's [explanations] come
 * first, for errors only it can tell apart.
 */
@Inject
class SourceErrorMessages(
    private val explanations: Set<SourceErrorExplanation>,
    private val network: NetworkConnectivity,
) {
    fun message(context: PlatformContext, error: Throwable): String =
        explanations.firstNotNullOfOrNull { it.explain(context, error) }
            ?: context.formattedSourceError(error, network)
}

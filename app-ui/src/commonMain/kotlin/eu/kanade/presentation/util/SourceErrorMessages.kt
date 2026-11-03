package eu.kanade.presentation.util

import dev.zacsweers.metro.Inject
import mihon.core.platform.PlatformContext
import mihon.core.platform.network.NetworkConnectivity

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

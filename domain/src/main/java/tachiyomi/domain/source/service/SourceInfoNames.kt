package tachiyomi.domain.source.service

/**
 * Names a source the way entry details show it, with its language when more than one is enabled, so modules without
 * the app's source preferences name it the same way.
 */
fun interface SourceInfoNames {
    fun of(sourceId: Long): String
}

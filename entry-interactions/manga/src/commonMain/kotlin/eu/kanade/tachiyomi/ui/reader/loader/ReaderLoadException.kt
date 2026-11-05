package eu.kanade.tachiyomi.ui.reader.loader

internal class ReaderLoadException(
    message: String,
    val canRetry: Boolean,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

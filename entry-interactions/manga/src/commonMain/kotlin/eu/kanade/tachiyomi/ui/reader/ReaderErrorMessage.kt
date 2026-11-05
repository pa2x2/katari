package eu.kanade.tachiyomi.ui.reader

/** The error as the reader shows it: its message, after its class's name unless that says nothing more. */
val Throwable.readerFormattedMessage: String
    get() = when (val className = this::class.simpleName) {
        null, "Exception", "IOException" -> message ?: className.orEmpty()
        else -> "$className: $message"
    }

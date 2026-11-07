package eu.kanade.tachiyomi.ui.reader.startup

import androidx.compose.runtime.Immutable
import eu.kanade.tachiyomi.ui.reader.loader.ReaderLoadException

@Immutable
internal sealed interface ReaderStartupState {
    /** [step] is null until opening reports what it is doing. */
    data class Loading(val step: ReaderStartupStep? = null) : ReaderStartupState
    data object Ready : ReaderStartupState

    data class Failed(
        val message: String,
        val canRetry: Boolean,
    ) : ReaderStartupState
}

internal fun Throwable.toReaderStartupFailure(): ReaderStartupState.Failed {
    return ReaderStartupState.Failed(
        message = message?.takeIf(String::isNotBlank) ?: "The selected chapter could not be opened.",
        canRetry = (this as? ReaderLoadException)?.canRetry ?: true,
    )
}

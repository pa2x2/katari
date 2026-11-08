package eu.kanade.tachiyomi.ui.reader.startup

import androidx.compose.runtime.Immutable

/** What opening the first chapter waits on, named on the loading screen. */
@Immutable
internal sealed interface ReaderStartupStep {
    /** The app was started for this chapter and its extensions haven't loaded yet. */
    data object LoadingExtensions : ReaderStartupStep

    /** A downloaded or local chapter's file or folder is listed. */
    data object OpeningFile : ReaderStartupStep

    /** The source is asked for the chapter's pages; a page list cached from an earlier visit skips this. */
    data class AskingSource(val sourceName: String) : ReaderStartupStep
}

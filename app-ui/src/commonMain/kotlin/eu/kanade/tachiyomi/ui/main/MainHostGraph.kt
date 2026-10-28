package eu.kanade.tachiyomi.ui.main

import eu.kanade.domain.base.IncognitoPreferences
import eu.kanade.domain.source.interactor.GetIncognitoState
import mihon.entry.interactions.download.EntryDownloadRuntimeFeature
import mihon.presentation.kit.feedback.AppMessages
import tachiyomi.domain.library.service.LibraryPreferences

/** Graph entry point of what a window needs to host the home screen. */
interface MainHostGraph {
    val libraryPreferences: LibraryPreferences
    val incognitoPreferences: IncognitoPreferences
    val getIncognitoState: GetIncognitoState
    val entryDownloadRuntimeFeature: EntryDownloadRuntimeFeature
    val appMessages: AppMessages
    val launchCleanup: LaunchCleanup
}

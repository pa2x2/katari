package eu.kanade.tachiyomi.ui.main

import app.katari.entry.interactions.download.EntryDownloadRuntimeFeature
import app.katari.presentation.kit.feedback.AppMessages
import app.katari.ui.host.AppLaunch
import eu.kanade.domain.base.IncognitoPreferences
import eu.kanade.domain.source.interactor.GetIncognitoState
import tachiyomi.domain.library.service.LibraryPreferences

/** Graph entry point of what a window needs to host the home screen. */
interface MainHostGraph {
    val libraryPreferences: LibraryPreferences
    val incognitoPreferences: IncognitoPreferences
    val getIncognitoState: GetIncognitoState
    val entryDownloadRuntimeFeature: EntryDownloadRuntimeFeature
    val appMessages: AppMessages
    val appLaunch: AppLaunch
}

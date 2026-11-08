package eu.kanade.presentation.more.settings.screen.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import app.katari.ui.di.LocalAppUiGraph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tachiyomi.domain.storage.service.StoragePreferences
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** [StoragePreferences.pickedStorageDirectory], kept current as the user picks a folder. */
@Composable
fun collectPickedStorageDirectory(storagePreferences: StoragePreferences): String? {
    val changes = remember(storagePreferences) { storagePreferences.pickedStorageDirectoryChanges() }
    val initial = remember(storagePreferences) { storagePreferences.pickedStorageDirectory() }
    val picked by changes.collectAsState(initial)
    return picked
}

/**
 * [location] as a path the user recognises, or the invalid-location message where it can't be resolved. Null while it
 * is resolved, so callers show nothing in place of a blank path.
 */
@Composable
fun storageLocationDisplayPath(location: String): String? {
    val paths = LocalAppUiGraph.current.storageLocationPaths
    val invalidLocation = stringResource(MR.strings.invalid_location, location)
    // Resolving a document provider's path can query the provider
    val displayPath by produceState<String?>(null, location, invalidLocation) {
        value = withContext(Dispatchers.IO) {
            paths.displayablePath(location)
        } ?: invalidLocation
    }
    return displayPath
}

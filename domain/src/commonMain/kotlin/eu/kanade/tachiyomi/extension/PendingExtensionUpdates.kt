package eu.kanade.tachiyomi.extension

import kotlinx.coroutines.flow.Flow

/** The installed extensions that have an update in their store, which the Browse tab's badge counts. */
interface PendingExtensionUpdates {
    val count: Flow<Int>

    /** True while the extensions are updating themselves, when the badge holds back its count. */
    val isAutoUpdateInProgress: Flow<Boolean>
}

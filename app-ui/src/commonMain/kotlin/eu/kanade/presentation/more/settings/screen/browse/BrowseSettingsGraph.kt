package eu.kanade.presentation.more.settings.screen.browse

import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.ui.security.SettingChangeAuthentication
import mihon.core.common.CustomPreferences
import mihon.domain.extension.interactor.GetExtensionStoreCountAsFlow
import mihon.entry.interactions.media.EntryPreviewFeature
import tachiyomi.domain.library.service.DuplicatePreferences

/** Graph entry points of the browse settings and their duplicate detection. */
interface BrowseSettingsGraph {
    val sourcePreferences: SourcePreferences
    val customPreferences: CustomPreferences
    val entryPreviewFeature: EntryPreviewFeature
    val getExtensionStoreCountAsFlow: GetExtensionStoreCountAsFlow
    val settingChangeAuthentication: SettingChangeAuthentication
    val duplicatePreferences: DuplicatePreferences
}

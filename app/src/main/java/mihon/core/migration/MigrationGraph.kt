package mihon.core.migration

import android.app.Application
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.source.service.GlobalSourcePreferences
import mihon.domain.extension.repository.ExtensionStoreRepository
import mihon.feature.profiles.core.ProfileDatabase
import mihon.feature.profiles.core.ProfileStore
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.repository.CategoryRepository
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.update.repository.LibraryUpdateRulesRepository

/**
 * What the app version migrations read from the graph. Members are resolved when a migration reads them, so startup
 * pays only for what the pending migrations use.
 */
interface MigrationGraph {
    val application: Application
    val basePreferences: BasePreferences
    val globalSourcePreferences: GlobalSourcePreferences
    val libraryPreferences: LibraryPreferences
    val downloadPreferences: DownloadPreferences
    val profileStore: ProfileStore
    val profileDatabase: ProfileDatabase
    val getCategories: GetCategories
    val categoryRepository: CategoryRepository
    val libraryUpdateRulesRepository: LibraryUpdateRulesRepository
    val extensionStoreRepository: ExtensionStoreRepository
}

package app.katari.sync.ui.arrival.change

import app.katari.profiles.core.ProfileManager
import app.katari.sync.ui.arrival.change.named.NamedSetting
import app.katari.sync.ui.arrival.change.named.NamedSettings
import dev.zacsweers.metro.Inject
import eu.kanade.domain.source.model.SourceFeedPreset
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.extension.ExtensionManagement
import eu.kanade.tachiyomi.source.entry.EntryCatalogueSource
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.entry.interactor.GetEntryById
import tachiyomi.domain.source.service.SourceManager

/**
 * What telling an arrived change needs from the rest of the app: names of sources and categories, stored feeds, and the
 * settings no Settings row stores.
 */
@Inject
class ChangeLookups(
    private val sourceManager: SourceManager,
    private val getCategories: GetCategories,
    private val json: Json,
    private val activeSourcePreferences: SourcePreferences,
    private val namedSettings: NamedSettings,
    private val extensionManagement: ExtensionManagement,
    private val getEntryById: GetEntryById,
    private val profileManager: ProfileManager,
) {
    internal fun namedSetting(key: String): NamedSetting? = namedSettings.of(key)

    fun sourceName(sourceKey: Long): String = sourceManager.getDisplayInfo(sourceKey).name

    /** The name of the profile with [profileId]; null when it is gone. */
    fun profileName(profileId: Long): String? = profileManager.profiles.value.find { it.id == profileId }?.name

    /** The installed extension's name, or its package name once it is uninstalled. */
    fun extensionName(pkgName: String): String = extensionManagement.installed(pkgName)?.name ?: pkgName

    /** The title of the entry with [id] here; null when it is no longer here. */
    suspend fun entryTitle(id: Long): String? = getEntryById.await(id)?.title

    /** The language of the source's catalog, which tells apart sources of one name; null for a source without one. */
    fun sourceLanguage(sourceKey: Long): String? = (sourceManager.getOrStub(sourceKey) as? EntryCatalogueSource)?.lang

    /** The categories of the profile with [profileId], or of the active one for a setting all profiles share. */
    fun categories(profileId: Long?): Flow<List<Category>> =
        profileId?.let(getCategories::subscribeForProfile) ?: getCategories.subscribe()

    /** The feed presets saved now, which name the feeds whose presets didn't change. */
    fun savedFeedPresets(): List<SourceFeedPreset> = activeSourcePreferences.savedFeedPresets.get()

    /** Source preferences read from [store], such as a value before a sync changed it. */
    fun sourcePreferences(store: PreferenceStore): SourcePreferences = SourcePreferences(store, json)
}

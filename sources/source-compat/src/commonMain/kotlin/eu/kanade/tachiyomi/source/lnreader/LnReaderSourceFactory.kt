package eu.kanade.tachiyomi.source.lnreader

import android.content.SharedPreferences
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.source.entry.ConfigurableSource
import eu.kanade.tachiyomi.source.entry.EntryPreferenceScreen
import eu.kanade.tachiyomi.source.entry.SiteStorage
import eu.kanade.tachiyomi.source.entry.SiteStorageSource
import eu.kanade.tachiyomi.source.lnreader.runtime.LnReaderPluginCalls
import eu.kanade.tachiyomi.source.lnreader.runtime.LnReaderPluginFile
import eu.kanade.tachiyomi.source.lnreader.runtime.LnReaderPlugins
import eu.kanade.tachiyomi.source.lnreader.settings.lnReaderSettings
import mihon.script.runtime.ScriptNetwork

/** Makes book sources of LNReader plugins, for the extension loader. */
@Inject
class LnReaderSourceFactory(private val plugins: LnReaderPlugins, private val network: ScriptNetwork) {

    /** Loads [file] in [profileId] and returns what the loader keeps beside it. Throws when the plugin doesn't load. */
    suspend fun describe(file: LnReaderPluginFile, profileId: Long): LnReaderPluginDescription =
        plugins.use(file, profileId) { plugin ->
            LnReaderPluginDescription.of(plugin.description(), readsLatest = "showLatestNovels" in file.source.code)
        }

    /**
     * The source of [file] under the local [key]. No runtime opens here; the first call loads the plugin in the profile
     * it runs for, the active one ([activeProfileId]) unless the work that calls says otherwise.
     */
    fun create(
        file: LnReaderPluginFile,
        key: Long,
        description: LnReaderPluginDescription,
        lang: String,
        customCss: String?,
        activeProfileId: () -> Long,
    ): LnReaderSource {
        val settings = lnReaderSettings(description.settings)
        val setup = LnReaderSourceSetup(
            file = file,
            key = key,
            description = description,
            lang = lang,
            customCss = customCss,
            calls = LnReaderPluginCalls(file, plugins, activeProfileId),
            client = network.client,
            settings = settings,
        )
        // Katari offers settings and the site's storage by the source's type, so each combination is a class
        return when {
            settings.isNotEmpty() && description.webStorage -> LnReaderSettingsSiteStorageSource(setup)
            settings.isNotEmpty() -> LnReaderSettingsSource(setup)
            description.webStorage -> LnReaderSiteStorageSource(setup)
            else -> LnReaderSource(setup)
        }
    }
}

private class LnReaderSettingsSource(setup: LnReaderSourceSetup) :
    LnReaderSource(setup),
    ConfigurableSource {
    override fun getSourcePreferences(): SharedPreferences = settingsFile()

    override fun setupPreferenceScreen(screen: EntryPreferenceScreen) = setupSettings(screen)
}

private class LnReaderSiteStorageSource(setup: LnReaderSourceSetup) :
    LnReaderSource(setup),
    SiteStorageSource {
    override fun saveSiteStorage(storage: SiteStorage) = keepSiteStorage(storage)
}

private class LnReaderSettingsSiteStorageSource(setup: LnReaderSourceSetup) :
    LnReaderSource(setup),
    ConfigurableSource,
    SiteStorageSource {
    override fun getSourcePreferences(): SharedPreferences = settingsFile()

    override fun setupPreferenceScreen(screen: EntryPreferenceScreen) = setupSettings(screen)

    override fun saveSiteStorage(storage: SiteStorage) = keepSiteStorage(storage)
}

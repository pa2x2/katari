package eu.kanade.tachiyomi.data.backup.conversion.lnreader

import app.katari.data.extension.model.lnreader.lnReaderStore
import eu.kanade.tachiyomi.data.backup.models.BackupExtensionStore
import eu.kanade.tachiyomi.data.backup.models.BackupSource
import eu.kanade.tachiyomi.data.backup.models.backupExtensionStoreMapper
import tachiyomi.domain.source.identity.SourceIdScheme
import tachiyomi.domain.source.identity.SourceProviderKind
import tachiyomi.domain.source.identity.lnReaderExternalId

/**
 * The plugins of an LNReader backup as Katari's sources, and the stores they come from. A plugin's source is keyed by
 * its external id, which no other source in the converted backup can have.
 *
 * LNReader keeps its repositories out of backups, so each store is worked out from the plugins' addresses (D077): a
 * plugin's script sits under its repository's `plugins/v<version>/` tree, whose index LNReader reads at
 * `.dist/plugins.min.json`.
 *
 * @param installed the plugins `Plugins.json` lists, which name the sources.
 * @param listedUrls the scripts of every plugin the repositories offered, from the settings' `AVAILABLE_PLUGINS`.
 */
internal class LnReaderPlugins(installed: List<LnReaderPlugin>, listedUrls: List<String>) {
    private val nameById = installed.associate { it.id to it.name }

    val backupStores: List<BackupExtensionStore> = (installed.map { it.url } + listedUrls)
        .mapNotNull { PLUGIN_TREE.find(it)?.value }
        .distinct()
        .map { tree -> backupExtensionStoreMapper(lnReaderStore("$tree$INDEX_PATH")) }

    /** The sources of the plugins named by [pluginIds]. */
    fun backupSources(pluginIds: Collection<String>): List<BackupSource> = pluginIds.map { id ->
        BackupSource(
            name = nameById[id]?.takeIf(String::isNotEmpty) ?: id,
            sourceId = sourceKey(id),
            idScheme = SourceIdScheme.LNREADER.key,
            externalId = lnReaderExternalId(id),
            providerKind = SourceProviderKind.LNREADER_PLUGIN.key,
        )
    }

    fun sourceKey(pluginId: String): Long = lnReaderExternalId(pluginId)

    private companion object {
        // The first plugins/v<version>/ of the address: the script's own path can hold "plugins" again
        val PLUGIN_TREE = Regex("""^https?://.+?/plugins/v\d[^/]*/""")
        const val INDEX_PATH = ".dist/plugins.min.json"
    }
}

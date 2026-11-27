package eu.kanade.tachiyomi.data.backup.conversion.lnreader

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.data.backup.conversion.BackupFileContents
import eu.kanade.tachiyomi.data.backup.conversion.BackupFormat
import eu.kanade.tachiyomi.data.backup.conversion.lnreader.settings.LnReaderStoredSettings
import eu.kanade.tachiyomi.data.backup.conversion.lnreader.settings.convertLnReaderSettings
import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.models.BackupEntry
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okio.BufferedSource
import java.time.ZoneId
import java.util.zip.ZipInputStream

/**
 * LNReader's backups, the zip of JSON files of format 2 (LNReader 2.1 and later). Novels come before the categories,
 * settings and plugins in the zip, so the zip is read twice: everything but the novels first, then the novels one at a
 * time, which keeps a library of hundreds of thousands of chapters to one novel in memory.
 */
@Inject
@ContributesIntoSet(AppScope::class)
class LnReaderBackupFormat(private val json: Json) : BackupFormat {
    override val appName = "LNReader"

    override val fileReads = 2

    override val relatedAppNames = emptyList<String>()

    override fun recognizes(contents: BackupFileContents): Boolean =
        contents is BackupFileContents.Zip &&
            VERSION in contents.entryNames &&
            contents.entryNames.any { it == CATEGORIES || it.startsWith(NOVEL_PREFIX) }

    override suspend fun convert(
        open: () -> BufferedSource,
        writeEntry: (BackupEntry) -> Unit,
    ): BackupFormat.ConvertedContent {
        var version: LnReaderVersion? = null
        var categoryList = emptyList<LnReaderCategory>()
        var installed = emptyList<LnReaderPlugin>()
        var settings = JsonObject(emptyMap())
        var covers = false
        var pluginCode = false
        var downloads = false
        open().zipEntries { name, zip ->
            when {
                name == VERSION -> version = zip.decode(LnReaderVersion.serializer())
                name == CATEGORIES -> categoryList = zip.decode(ListSerializer(LnReaderCategory.serializer()))
                name == PLUGINS -> installed = zip.decode(ListSerializer(LnReaderPlugin.serializer()))
                name == SETTINGS -> settings = zip.decode(JsonObject.serializer())
                name == PLUGIN_CODE -> pluginCode = true
                name == DOWNLOADS -> downloads = true
                name.startsWith(COVER_PREFIX) -> covers = true
            }
        }
        requireNotNull(version?.formatVersion) { "Not an LNReader backup of format 2" }

        val stored = LnReaderStoredSettings(settings, json)
        val categories = LnReaderCategories(categoryList)
        val plugins = LnReaderPlugins(installed, stored.availablePluginUrls)
        val zone = ZoneId.systemDefault()
        val report = LnReaderConversionReport()
        val pluginIds = linkedSetOf<String>()
        open().zipEntries { name, zip ->
            if (!name.startsWith(NOVEL_PREFIX)) return@zipEntries
            val novel = zip.decode(LnReaderNovel.serializer())
            val chapterList = stored.chapterList(novel)
            report.add(novel, chapterList, tracked = novel.id in stored.trackedNovelIds)
            // D078
            if (novel.isLocal) return@zipEntries
            pluginIds += novel.pluginId
            writeEntry(
                novel.toBackupEntry(plugins.sourceKey(novel.pluginId), categories.of(novel.id), zone, chapterList),
            )
        }

        val sources = plugins.backupSources(pluginIds)
        val convertedSettings = convertLnReaderSettings(
            stored,
            appName,
            categoryIds = categories.backupCategories.mapTo(hashSetOf()) { it.id },
            sources = sources,
            sourceKey = plugins::sourceKey,
        )
        val metadata = Backup(
            backupCategories = categories.backupCategories,
            backupSources = sources,
            backupPreferences = convertedSettings.preferences,
            backupExtensionStores = plugins.backupStores,
        )
        return BackupFormat.ConvertedContent(
            metadata,
            report.build(
                categories,
                sources,
                plugins.backupStores,
                LnReaderFiles(covers, pluginCode, downloads),
                convertedSettings,
            ),
        )
    }

    private fun <T> ZipInputStream.decode(deserializer: DeserializationStrategy<T>): T =
        json.decodeFromString(deserializer, readBytes().decodeToString())

    private companion object {
        const val VERSION = "Version.json"
        const val CATEGORIES = "Category.json"
        const val PLUGINS = "Plugins.json"
        const val SETTINGS = "Setting.json"
        const val NOVEL_PREFIX = "NovelAndChapters/"
        const val COVER_PREFIX = "Covers/"
        const val PLUGIN_CODE = "plugins.zip"
        const val DOWNLOADS = "novel-files.zip"
    }
}

private inline fun BufferedSource.zipEntries(block: (name: String, zip: ZipInputStream) -> Unit) {
    ZipInputStream(inputStream()).use { zip ->
        while (true) {
            val entry = zip.nextEntry ?: break
            block(entry.name, zip)
        }
    }
}

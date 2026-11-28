package eu.kanade.tachiyomi.data.backup.conversion.lnreader

import app.katari.core.platform.PlatformContext
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.data.backup.conversion.BackupFileContents
import eu.kanade.tachiyomi.data.backup.conversion.BackupFormat
import eu.kanade.tachiyomi.data.backup.conversion.KatariDefaultCategory
import eu.kanade.tachiyomi.data.backup.conversion.lnreader.settings.LnReaderStoredSettings
import eu.kanade.tachiyomi.data.backup.conversion.lnreader.settings.convertLnReaderSettings
import eu.kanade.tachiyomi.data.backup.conversion.newer.FILE_PART
import eu.kanade.tachiyomi.data.backup.conversion.newer.IN_BACKUP
import eu.kanade.tachiyomi.data.backup.conversion.newer.IN_ENTRY
import eu.kanade.tachiyomi.data.backup.conversion.newer.JsonShape
import eu.kanade.tachiyomi.data.backup.conversion.newer.NewerAppBackupException
import eu.kanade.tachiyomi.data.backup.conversion.newer.NewerAppReading
import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.models.BackupEntry
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
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
class LnReaderBackupFormat(private val context: PlatformContext, private val json: Json) : BackupFormat {
    override val appName = "LNReader"

    override val fileReads = 2

    override val relatedAppNames = emptyList<String>()

    override fun recognizes(contents: BackupFileContents): Boolean =
        contents is BackupFileContents.Zip &&
            VERSION in contents.entryNames &&
            contents.entryNames.any { it == CATEGORIES || it.startsWith(NOVEL_PREFIX) }

    override suspend fun convert(
        open: () -> BufferedSource,
        app: String,
        writeEntry: (BackupEntry) -> Unit,
    ): BackupFormat.ConvertedContent {
        val reading = NewerAppReading(app)
        var version: LnReaderVersion? = null
        var categoryList = emptyList<LnReaderCategory>()
        var installed = emptyList<LnReaderPlugin>()
        var settings = JsonObject(emptyMap())
        var covers = false
        var pluginCode = false
        var downloads = false
        open().zipEntries { name, zip ->
            when {
                name == VERSION ->
                    version =
                        zip.readWhole(reading, lnReaderVersionShape(), IN_BACKUP, LnReaderVersion.serializer())
                name == CATEGORIES -> categoryList = zip.readWhole(
                    reading,
                    lnReaderCategoriesShape(),
                    IN_BACKUP + "categories",
                    ListSerializer(LnReaderCategory.serializer()),
                )
                name == PLUGINS -> installed = zip.readWhole(
                    reading,
                    lnReaderPluginsShape(),
                    IN_BACKUP + "plugins",
                    ListSerializer(LnReaderPlugin.serializer()),
                )
                // Settings are free-form; a key Katari doesn't know is listed with them (D118)
                name == SETTINGS -> settings = zip.decode(JsonObject.serializer())
                name == PLUGIN_CODE -> pluginCode = true
                name == DOWNLOADS -> downloads = true
                name.startsWith(COVER_PREFIX) -> covers = true
                name.startsWith(NOVEL_PREFIX) -> Unit
                else -> reading.unknown(IN_BACKUP, name, FILE_PART)
            }
        }
        val formatVersion = requireNotNull(version?.formatVersion) { "Not an LNReader backup of format 2" }
        reading.formatVersion = formatVersion
        reading.appVersion = version.appVersion
        if (formatVersion > FORMAT_VERSION) throw NewerAppBackupException(app)

        val stored = LnReaderStoredSettings(settings, json)
        val categories = LnReaderCategories(KatariDefaultCategory.of(context, app), categoryList)
        val plugins = LnReaderPlugins(installed, stored.availablePluginUrls)
        val zone = ZoneId.systemDefault()
        val report = LnReaderConversionReport()
        val pluginIds = linkedSetOf<String>()
        val novelShape = lnReaderNovelShape()
        open().zipEntries { name, zip ->
            if (!name.startsWith(NOVEL_PREFIX)) return@zipEntries
            val novel = zip.readEntry(reading, novelShape) ?: return@zipEntries
            val chapterList = stored.chapterList(novel)
            // LNReader backs up every novel it has opened; outside the library Katari keeps those with progress (D025)
            if (!novel.inLibrary && !novel.hasProgress) return@zipEntries
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
            app,
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
            reading.findings(),
        )
    }

    private fun <T> ZipInputStream.decode(deserializer: DeserializationStrategy<T>): T =
        json.decodeFromString(deserializer, readBytes().decodeToString())

    // Text that isn't JSON fails while checking, as a damaged file; JSON that doesn't decode is a newer LNReader's
    private fun <T> ZipInputStream.readWhole(
        reading: NewerAppReading,
        shape: JsonShape,
        where: List<String>,
        deserializer: DeserializationStrategy<T>,
    ): T {
        val text = readBytes().decodeToString()
        lateinit var element: JsonElement
        return reading.whole(
            check = {
                element = json.parseToJsonElement(text)
                shape.check(element, where, reading)
            },
            decode = { json.decodeFromJsonElement(deserializer, element) },
        )
    }

    private fun ZipInputStream.readEntry(reading: NewerAppReading, shape: JsonShape): LnReaderNovel? {
        val text = readBytes().decodeToString()
        lateinit var element: JsonElement
        return reading.entry(
            title = { (element as? JsonObject)?.get("name")?.jsonPrimitive?.contentOrNull },
            check = {
                element = json.parseToJsonElement(text)
                shape.check(element, IN_ENTRY, reading)
            },
            decode = { json.decodeFromJsonElement(LnReaderNovel.serializer(), element) },
        )
    }

    private companion object {
        /** The newest format Katari reads; LNReader 2.1 writes it. */
        const val FORMAT_VERSION = 2

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

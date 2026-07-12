package tachiyomi.source.local

import android.content.Context
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.entry.EmptyChapterListSource
import eu.kanade.tachiyomi.source.entry.EntryCatalogueSource
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.EntryImagePage
import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.EntryPageResult
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.PlaybackSelection
import eu.kanade.tachiyomi.source.entry.SEntry
import eu.kanade.tachiyomi.source.entry.SEntryChapter
import eu.kanade.tachiyomi.source.entry.UnmeteredSource
import eu.kanade.tachiyomi.util.lang.compareToCaseInsensitiveNaturalOrder
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import logcat.LogPriority
import mihon.core.archive.archiveReader
import mihon.core.archive.epubReader
import nl.adaptivity.xmlutil.core.AndroidXmlReader
import nl.adaptivity.xmlutil.serialization.XML
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.storage.extension
import tachiyomi.core.common.storage.nameWithoutExtension
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.ImageUtil
import tachiyomi.core.common.util.system.logcat
import tachiyomi.core.metadata.comicinfo.COMIC_INFO_FILE
import tachiyomi.core.metadata.comicinfo.ComicInfo
import tachiyomi.core.metadata.tachiyomi.MangaDetails
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.service.ChapterRecognition
import tachiyomi.i18n.MR
import tachiyomi.source.local.filter.OrderBy
import tachiyomi.source.local.image.LocalCoverManager
import tachiyomi.source.local.io.Archive
import tachiyomi.source.local.io.Format
import tachiyomi.source.local.io.LocalSourceFileSystem
import tachiyomi.source.local.metadata.fillMetadata
import uy.kohesive.injekt.injectLazy
import java.io.InputStream
import java.nio.charset.StandardCharsets
import kotlin.time.Duration.Companion.days
import tachiyomi.domain.source.model.Source as DomainSource

actual class LocalSource(
    private val context: Context,
    private val fileSystem: LocalSourceFileSystem,
    private val coverManager: LocalCoverManager,
) : EntryCatalogueSource, EmptyChapterListSource, UnmeteredSource {

    private val json: Json by injectLazy()
    private val xml: XML by injectLazy()

    @Suppress("PrivatePropertyName")
    private val PopularFilters = EntryFilterList(OrderBy.Popular(context))

    @Suppress("PrivatePropertyName")
    private val LatestFilters = EntryFilterList(OrderBy.Latest(context))

    override val name: String = context.stringResource(MR.strings.local_source)

    override val id: Long = ID

    override val lang: String = "other"

    override fun toString() = name

    override val supportsLatest: Boolean = true

    // Browse related
    override suspend fun getPopularContent(page: Int) = getSearchContent(page, "", PopularFilters)

    override suspend fun getLatestUpdates(page: Int) = getSearchContent(page, "", LatestFilters)

    override suspend fun getSearchContent(
        page: Int,
        query: String,
        filters: EntryFilterList,
    ): EntryPageResult<SEntry> = withIOContext {
        val lastModifiedLimit = if (filters === LatestFilters) {
            System.currentTimeMillis() - LATEST_THRESHOLD
        } else {
            0L
        }

        var mangaDirs = fileSystem.getFilesInBaseDirectory()
            // Filter out files that are hidden and is not a folder
            .filter { it.isDirectory && !it.name.orEmpty().startsWith('.') }
            .distinctBy { it.name }
            .filter {
                if (lastModifiedLimit == 0L && query.isBlank()) {
                    true
                } else if (lastModifiedLimit == 0L) {
                    it.name.orEmpty().contains(query, ignoreCase = true)
                } else {
                    it.lastModified() >= lastModifiedLimit
                }
            }

        filters.forEach { filter ->
            when (filter) {
                is OrderBy.Popular -> {
                    mangaDirs = if (filter.state!!.ascending) {
                        mangaDirs.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name.orEmpty() })
                    } else {
                        mangaDirs.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.name.orEmpty() })
                    }
                }
                is OrderBy.Latest -> {
                    mangaDirs = if (filter.state!!.ascending) {
                        mangaDirs.sortedBy(UniFile::lastModified)
                    } else {
                        mangaDirs.sortedByDescending(UniFile::lastModified)
                    }
                }
                else -> {
                    /* Do nothing */
                }
            }
        }

        val entries = mangaDirs
            .map { mangaDir ->
                async {
                    LocalEntryMetadata(
                        title = mangaDir.name.orEmpty(),
                        url = mangaDir.name.orEmpty(),
                    ).apply {
                        // Try to find the cover
                        coverManager.find(mangaDir.name.orEmpty())?.let {
                            thumbnailUrl = it.uri.toString()
                        }
                    }.toSEntry()
                }
            }
            .awaitAll()

        EntryPageResult(entries, false)
    }

    override suspend fun getContentDetails(entry: SEntry): SEntry {
        require(entry.type == EntryType.MANGA) { "Local source only supports manga entries" }
        return getEntryDetails(entry.toLocalEntryMetadata()).toSEntry()
    }

    override suspend fun getChapterList(entry: SEntry): List<SEntryChapter> {
        require(entry.type == EntryType.MANGA) { "Local source only supports manga entries" }
        return getChapterMetadataList(entry.toLocalEntryMetadata()).map { it.toSEntryChapter() }
    }

    override suspend fun getMedia(
        chapter: SEntryChapter,
        selection: PlaybackSelection,
    ): EntryMedia = withIOContext {
        EntryMedia.ImagePages(getImagePages(chapter.url))
    }

    // Entry details related
    private suspend fun getEntryDetails(entry: LocalEntryMetadata): LocalEntryMetadata = withIOContext {
        coverManager.find(entry.url)?.let {
            entry.thumbnailUrl = it.uri.toString()
        }

        // Augment entry details based on metadata files
        try {
            val mangaDir = fileSystem.getMangaDirectory(entry.url) ?: error("${entry.url} is not a valid directory")
            val mangaDirFiles = mangaDir.listFiles().orEmpty()

            val comicInfoFile = mangaDirFiles
                .firstOrNull { it.name == COMIC_INFO_FILE }
            val noXmlFile = mangaDirFiles
                .firstOrNull { it.name == ".noxml" }
            val legacyJsonDetailsFile = mangaDirFiles
                .firstOrNull { it.extension == "json" }

            when {
                // Top level ComicInfo.xml
                comicInfoFile != null -> {
                    noXmlFile?.delete()
                    setEntryDetailsFromComicInfoFile(comicInfoFile.openInputStream(), entry)
                }

                // Old custom JSON format
                // TODO: remove support for this entirely after a while
                legacyJsonDetailsFile != null -> {
                    json.decodeFromStream<MangaDetails>(legacyJsonDetailsFile.openInputStream()).run {
                        title?.let { entry.title = it }
                        author?.let { entry.author = it }
                        artist?.let { entry.artist = it }
                        description?.let { entry.description = it }
                        genre?.let { entry.genre = it }
                        status?.let { entry.status = it }
                    }
                    // Replace with ComicInfo.xml file
                    val comicInfo = entry.getComicInfo()
                    mangaDir
                        .createFile(COMIC_INFO_FILE)
                        ?.openOutputStream()
                        ?.use {
                            val comicInfoString = xml.encodeToString(ComicInfo.serializer(), comicInfo)
                            it.write(comicInfoString.toByteArray())
                            legacyJsonDetailsFile.delete()
                        }
                }

                // Copy ComicInfo.xml from chapter archive to top level if found
                noXmlFile == null -> {
                    val chapterArchives = mangaDirFiles.filter(Archive::isSupported)

                    val copiedFile = copyComicInfoFileFromChapters(chapterArchives, mangaDir)
                    if (copiedFile != null) {
                        setEntryDetailsFromComicInfoFile(copiedFile.openInputStream(), entry)
                    } else {
                        // Avoid re-scanning
                        mangaDir.createFile(".noxml")
                    }
                }
            }
        } catch (e: Throwable) {
            logcat(LogPriority.ERROR, e) { "Error setting entry details from local metadata for ${entry.title}" }
        }

        return@withIOContext entry
    }

    private fun <T> getComicInfoForChapter(chapter: UniFile, block: (InputStream) -> T): T? {
        return if (chapter.isDirectory) {
            chapter.findFile(COMIC_INFO_FILE)?.openInputStream()?.use(block)
        } else {
            chapter.archiveReader(context).use { reader ->
                reader.getInputStream(COMIC_INFO_FILE)?.use(block)
            }
        }
    }

    private fun copyComicInfoFileFromChapters(chapterArchives: List<UniFile>, folder: UniFile): UniFile? {
        for (chapter in chapterArchives) {
            val file = getComicInfoForChapter(chapter) f@{ stream ->
                return@f copyComicInfoFile(stream, folder)
            }
            if (file != null) return file
        }
        return null
    }

    private fun copyComicInfoFile(comicInfoFileStream: InputStream, folder: UniFile): UniFile? {
        return folder.createFile(COMIC_INFO_FILE)?.apply {
            openOutputStream().use { outputStream ->
                comicInfoFileStream.use { it.copyTo(outputStream) }
            }
        }
    }

    private fun parseComicInfo(stream: InputStream): ComicInfo {
        return AndroidXmlReader(stream, StandardCharsets.UTF_8.name()).use {
            xml.decodeFromReader<ComicInfo>(it)
        }
    }

    private fun setEntryDetailsFromComicInfoFile(stream: InputStream, entry: LocalEntryMetadata) {
        entry.copyFromComicInfo(parseComicInfo(stream))
    }

    private fun setChapterDetailsFromComicInfoFile(stream: InputStream, chapter: LocalEntryChapterMetadata) {
        val comicInfo = parseComicInfo(stream)

        comicInfo.title?.let { chapter.name = it.value }
        comicInfo.number?.value?.toDoubleOrNull()?.let { chapter.chapterNumber = it }
        comicInfo.translator?.let { chapter.scanlator = it.value }
    }

    // Chapters
    private suspend fun getChapterMetadataList(
        entry: LocalEntryMetadata,
    ): List<LocalEntryChapterMetadata> = withIOContext {
        val chapters = fileSystem.getFilesInMangaDirectory(entry.url)
            // Only keep supported formats
            .filterNot { it.name.orEmpty().startsWith('.') }
            .filter { it.isDirectory || Archive.isSupported(it) || it.extension.equals("epub", true) }
            .map { chapterFile ->
                LocalEntryChapterMetadata(
                    url = "${entry.url}/${chapterFile.name}",
                    name = if (chapterFile.isDirectory) {
                        chapterFile.name
                    } else {
                        chapterFile.nameWithoutExtension
                    }.orEmpty(),
                    dateUpload = chapterFile.lastModified(),
                ).apply {
                    chapterNumber = ChapterRecognition
                        .parseChapterNumber(entry.title, name, chapterNumber)

                    val format = Format.valueOf(chapterFile)
                    if (format is Format.Epub) {
                        format.file.epubReader(context).use { epub ->
                            epub.fillMetadata(entry, this)
                        }
                    } else {
                        getComicInfoForChapter(chapterFile) { stream ->
                            setChapterDetailsFromComicInfoFile(stream, this)
                        }
                    }
                }
            }
            .sortedWith { c1, c2 ->
                c2.name.compareToCaseInsensitiveNaturalOrder(c1.name)
            }

        // Copy the cover from the first chapter found if not available
        if (entry.thumbnailUrl.isNullOrBlank()) {
            chapters.lastOrNull()?.let { chapter ->
                updateCover(chapter, entry)
            }
        }

        chapters
    }

    // Filters
    override fun getFilterList() = EntryFilterList(OrderBy.Popular(context))

    fun getFormat(chapterUrl: String): Format {
        try {
            val (mangaDirName, chapterName) = chapterUrl.split('/', limit = 2)
            return fileSystem.getBaseDirectory()
                ?.findFile(mangaDirName)
                ?.findFile(chapterName)
                ?.let(Format.Companion::valueOf)
                ?: throw Exception(context.stringResource(MR.strings.chapter_not_found))
        } catch (e: Format.UnknownFormatException) {
            throw Exception(context.stringResource(MR.strings.local_invalid_format))
        } catch (e: Exception) {
            throw e
        }
    }

    private fun getImagePages(chapterUrl: String): List<EntryImagePage> {
        return when (val format = getFormat(chapterUrl)) {
            is Format.Directory -> {
                format.file.listFiles()
                    .orEmpty()
                    .sortedWith { f1, f2 ->
                        f1.name.orEmpty().compareToCaseInsensitiveNaturalOrder(f2.name.orEmpty())
                    }
                    .filter { !it.isDirectory && ImageUtil.isImage(it.name) { it.openInputStream() } }
                    .mapIndexed { index, file -> EntryImagePage(index, file.uri.toString(), file.uri.toString()) }
            }
            is Format.Archive -> {
                format.file.archiveReader(context).use { reader ->
                    reader.useEntries { entries ->
                        entries
                            .sortedWith { f1, f2 -> f1.name.compareToCaseInsensitiveNaturalOrder(f2.name) }
                            .filter { it.isFile && ImageUtil.isImage(it.name) { reader.getInputStream(it.name)!! } }
                            .mapIndexed { index, entry -> EntryImagePage(index, entry.name) }
                            .toList()
                    }
                }
            }
            is Format.Epub -> {
                format.file.epubReader(context).use { epub ->
                    epub.getImagesFromPages()
                        .mapIndexed { index, image -> EntryImagePage(index, image) }
                }
            }
        }
    }

    private fun updateCover(chapter: LocalEntryChapterMetadata, entry: LocalEntryMetadata): UniFile? {
        return try {
            when (val format = getFormat(chapter.url)) {
                is Format.Directory -> {
                    val imageFile = format.file.listFiles()
                        ?.sortedWith { f1, f2 ->
                            f1.name.orEmpty().compareToCaseInsensitiveNaturalOrder(
                                f2.name.orEmpty(),
                            )
                        }
                        ?.find {
                            !it.isDirectory && ImageUtil.isImage(it.name) { it.openInputStream() }
                        }

                    imageFile?.let { coverManager.update(entry, it.openInputStream()) }
                }
                is Format.Archive -> {
                    format.file.archiveReader(context).use { reader ->
                        val imageFile = reader.useEntries { entries ->
                            entries
                                .sortedWith { f1, f2 -> f1.name.compareToCaseInsensitiveNaturalOrder(f2.name) }
                                .find { it.isFile && ImageUtil.isImage(it.name) { reader.getInputStream(it.name)!! } }
                        }

                        imageFile?.let { coverManager.update(entry, reader.getInputStream(it.name)!!) }
                    }
                }
                is Format.Epub -> {
                    format.file.epubReader(context).use { epub ->
                        val imageFile = epub.getImagesFromPages().firstOrNull()

                        imageFile?.let { coverManager.update(entry, epub.getInputStream(it)!!) }
                    }
                }
            }
        } catch (e: Throwable) {
            logcat(LogPriority.ERROR, e) { "Error updating cover for ${entry.title}" }
            null
        }
    }

    private fun LocalEntryMetadata.getComicInfo() = ComicInfo(
        series = ComicInfo.Series(title),
        summary = description?.let { ComicInfo.Summary(it) },
        writer = author?.let { ComicInfo.Writer(it) },
        penciller = artist?.let { ComicInfo.Penciller(it) },
        genre = genre?.joinToString(", ")?.let { ComicInfo.Genre(it) },
        publishingStatus = ComicInfo.PublishingStatusTachiyomi(toComicInfoPublishingStatus(status)),
        title = null,
        number = null,
        web = null,
        translator = null,
        inker = null,
        colorist = null,
        letterer = null,
        coverArtist = null,
        tags = null,
        categories = null,
        source = null,
    )

    private fun LocalEntryMetadata.copyFromComicInfo(comicInfo: ComicInfo) {
        comicInfo.series?.let { title = it.value }
        comicInfo.writer?.let { author = it.value }
        comicInfo.summary?.let { description = it.value }

        listOfNotNull(
            comicInfo.genre?.value,
            comicInfo.tags?.value,
            comicInfo.categories?.value,
        )
            .flatMap { it.split(", ") }
            .distinct()
            .map { it.trim() }
            .filterNot { it.isBlank() }
            .takeIf { it.isNotEmpty() }
            ?.let { genre = it }

        listOfNotNull(
            comicInfo.penciller?.value,
            comicInfo.inker?.value,
            comicInfo.colorist?.value,
            comicInfo.letterer?.value,
            comicInfo.coverArtist?.value,
        )
            .flatMap { it.split(", ") }
            .distinct()
            .joinToString(", ") { it.trim() }
            .takeIf { it.isNotEmpty() }
            ?.let { artist = it }

        status = toEntryStatus(comicInfo.publishingStatus?.value)
    }

    private fun toComicInfoPublishingStatus(status: Int): String {
        return when (status) {
            LocalEntryMetadata.ONGOING -> "Ongoing"
            LocalEntryMetadata.COMPLETED -> "Completed"
            LocalEntryMetadata.LICENSED -> "Licensed"
            LocalEntryMetadata.PUBLISHING_FINISHED -> "Publishing finished"
            LocalEntryMetadata.CANCELLED -> "Cancelled"
            LocalEntryMetadata.ON_HIATUS -> "On hiatus"
            else -> "Unknown"
        }
    }

    private fun toEntryStatus(status: String?): Int {
        return when (status) {
            "Ongoing" -> LocalEntryMetadata.ONGOING
            "Completed" -> LocalEntryMetadata.COMPLETED
            "Licensed" -> LocalEntryMetadata.LICENSED
            "Publishing finished" -> LocalEntryMetadata.PUBLISHING_FINISHED
            "Cancelled" -> LocalEntryMetadata.CANCELLED
            "On hiatus" -> LocalEntryMetadata.ON_HIATUS
            else -> LocalEntryMetadata.UNKNOWN
        }
    }

    companion object {
        const val ID = 0L
        const val HELP_URL = "https://mihon.app/docs/guides/local-source/"

        private val LATEST_THRESHOLD = 7.days.inWholeMilliseconds
    }
}

fun Entry.isLocal(): Boolean = source == LocalSource.ID

fun DomainSource.isLocal(): Boolean = id == LocalSource.ID

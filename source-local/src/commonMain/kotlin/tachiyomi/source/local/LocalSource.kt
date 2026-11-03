package tachiyomi.source.local

import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.source.entry.EmptyChapterListSource
import eu.kanade.tachiyomi.source.entry.EntryCatalogueSource
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.EntryImagePage
import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.EntryPageResult
import eu.kanade.tachiyomi.source.entry.EntryRefresh
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.PlaybackSelection
import eu.kanade.tachiyomi.source.entry.SEntry
import eu.kanade.tachiyomi.source.entry.SEntryChapter
import eu.kanade.tachiyomi.source.entry.SourceMetadata
import eu.kanade.tachiyomi.source.entry.UnmeteredSource
import eu.kanade.tachiyomi.util.lang.compareToCaseInsensitiveNaturalOrder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import logcat.LogPriority
import mihon.core.archive.ArchiveReader
import mihon.core.archive.EpubReader
import mihon.core.archive.LibarchiveUnavailableException
import nl.adaptivity.xmlutil.serialization.XML
import tachiyomi.core.common.storage.StorageFile
import tachiyomi.core.common.storage.extension
import tachiyomi.core.common.storage.nameWithoutExtension
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.isImageFile
import tachiyomi.core.common.util.system.logcat
import tachiyomi.core.metadata.comicinfo.COMIC_INFO_FILE
import tachiyomi.core.metadata.comicinfo.ComicInfo
import tachiyomi.core.metadata.tachiyomi.MangaDetails
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.service.ChapterRecognition
import tachiyomi.domain.source.identity.LOCAL_SOURCE_ID
import tachiyomi.i18n.*
import tachiyomi.source.local.filter.OrderBy
import tachiyomi.source.local.image.LocalCoverManager
import tachiyomi.source.local.io.Archive
import tachiyomi.source.local.io.ArchiveOpener
import tachiyomi.source.local.io.Format
import tachiyomi.source.local.io.LocalSourceFileSystem
import tachiyomi.source.local.metadata.comicInfoXmlReader
import tachiyomi.source.local.metadata.copyFromComicInfo
import tachiyomi.source.local.metadata.fillMetadata
import tachiyomi.source.local.metadata.getComicInfo
import java.io.InputStream
import kotlin.time.Duration.Companion.days
import tachiyomi.domain.source.model.Source as DomainSource

/**
 * The entries in the storage location's `local` folder: one per folder, with folders of images, archives and epubs
 * inside as its chapters. [localize] gives the text of the source's name, filters and errors.
 */
class LocalSource(
    private val fileSystem: LocalSourceFileSystem,
    private val coverManager: LocalCoverManager,
    private val archives: ArchiveOpener,
    private val json: Json,
    private val xml: XML,
    private val localize: (StringResource) -> String,
) : EntryCatalogueSource, EmptyChapterListSource, SourceMetadata, UnmeteredSource {

    @Suppress("PrivatePropertyName")
    private val PopularFilters = EntryFilterList(OrderBy.Popular(localize))

    @Suppress("PrivatePropertyName")
    private val LatestFilters = EntryFilterList(OrderBy.Latest(localize))

    override val name: String = localize(MR.strings.local_source)

    override val id: Long = ID

    override val lang: String = "other"

    override val supportedEntryTypes: Set<EntryType> = LOCAL_SOURCE_SUPPORTED_ENTRY_TYPES

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
                        mangaDirs.sortedBy(StorageFile::lastModified)
                    } else {
                        mangaDirs.sortedByDescending(StorageFile::lastModified)
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
                            thumbnailUrl = it.uri
                        }
                    }.toSEntry()
                }
            }
            .awaitAll()

        EntryPageResult(entries, false)
    }

    override suspend fun refresh(
        entry: SEntry,
        existingChapters: List<SEntryChapter>,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ): EntryRefresh {
        require(entry.type == EntryType.MANGA) { "Local source only supports manga entries" }
        return EntryRefresh(
            entry = if (fetchDetails) getEntryDetails(entry.toLocalEntryMetadata()).toSEntry() else entry,
            chapters = if (fetchChapters) {
                getChapterMetadataList(entry.toLocalEntryMetadata()).map { it.toSEntryChapter() }
            } else {
                existingChapters
            },
        )
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
            entry.thumbnailUrl = it.uri
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

    private fun <T> getComicInfoForChapter(chapter: StorageFile, block: (InputStream) -> T): T? {
        return if (chapter.isDirectory) {
            chapter.findFile(COMIC_INFO_FILE)?.openInputStream()?.use(block)
        } else {
            openArchive(chapter).use { reader ->
                reader.getInputStream(COMIC_INFO_FILE)?.use(block)
            }
        }
    }

    private fun copyComicInfoFileFromChapters(chapterArchives: List<StorageFile>, folder: StorageFile): StorageFile? {
        for (chapter in chapterArchives) {
            val file = getComicInfoForChapter(chapter) f@{ stream ->
                return@f copyComicInfoFile(stream, folder)
            }
            if (file != null) return file
        }
        return null
    }

    private fun copyComicInfoFile(comicInfoFileStream: InputStream, folder: StorageFile): StorageFile? {
        return folder.createFile(COMIC_INFO_FILE)?.apply {
            openOutputStream().use { outputStream ->
                comicInfoFileStream.use { it.copyTo(outputStream) }
            }
        }
    }

    private fun parseComicInfo(stream: InputStream): ComicInfo {
        return comicInfoXmlReader(stream).use {
            xml.decodeFromReader<ComicInfo>(it)
        }
    }

    private fun setEntryDetailsFromComicInfoFile(stream: InputStream, entry: LocalEntryMetadata) {
        entry.copyFromComicInfo(parseComicInfo(stream))
    }

    private fun setChapterDetailsFromComicInfoFile(stream: InputStream, chapter: LocalEntryChapterMetadata) {
        chapter.copyFromComicInfo(parseComicInfo(stream))
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

                    try {
                        val format = Format.valueOf(chapterFile)
                        if (format is Format.Epub) {
                            openEpub(format.file).use { epub ->
                                epub.fillMetadata(entry, this)
                            }
                        } else {
                            getComicInfoForChapter(chapterFile) { stream ->
                                setChapterDetailsFromComicInfoFile(stream, this)
                            }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        logcat(LogPriority.ERROR, e) { "Failed to read metadata for ${chapterFile.name}" }
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
    override fun getFilterList() = EntryFilterList(OrderBy.Popular(localize))

    fun getFormat(chapterUrl: String): Format {
        try {
            val (mangaDirName, chapterName) = chapterUrl.split('/', limit = 2)
            return fileSystem.getBaseDirectory()
                ?.findFile(mangaDirName)
                ?.findFile(chapterName)
                ?.let(Format.Companion::valueOf)
                ?: throw Exception(localize(MR.strings.chapter_not_found))
        } catch (e: Format.UnknownFormatException) {
            throw Exception(localize(MR.strings.local_invalid_format))
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
                    .filter { !it.isDirectory && isImageFile(it.name) { it.openInputStream() } }
                    .mapIndexed { index, file -> EntryImagePage(index, file.uri, file.uri) }
            }
            is Format.Archive -> {
                openArchive(format.file).use { reader ->
                    reader.useEntries { entries ->
                        entries
                            .sortedWith { f1, f2 -> f1.name.compareToCaseInsensitiveNaturalOrder(f2.name) }
                            .filter { it.isFile && isImageFile(it.name) { reader.getInputStream(it.name)!! } }
                            .mapIndexed { index, entry -> EntryImagePage(index, entry.name) }
                            .toList()
                    }
                }
            }
            is Format.Epub -> {
                openEpub(format.file).use { epub ->
                    epub.getImagesFromPages()
                        .mapIndexed { index, image -> EntryImagePage(index, image) }
                }
            }
        }
    }

    private fun updateCover(chapter: LocalEntryChapterMetadata, entry: LocalEntryMetadata): StorageFile? {
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
                            !it.isDirectory && isImageFile(it.name) { it.openInputStream() }
                        }

                    imageFile?.let { coverManager.update(entry, it.openInputStream()) }
                }
                is Format.Archive -> {
                    openArchive(format.file).use { reader ->
                        val imageFile = reader.useEntries { entries ->
                            entries
                                .sortedWith { f1, f2 -> f1.name.compareToCaseInsensitiveNaturalOrder(f2.name) }
                                .find { it.isFile && isImageFile(it.name) { reader.getInputStream(it.name)!! } }
                        }

                        imageFile?.let { coverManager.update(entry, reader.getInputStream(it.name)!!) }
                    }
                }
                is Format.Epub -> {
                    openEpub(format.file).use { epub ->
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

    private fun openEpub(file: StorageFile): EpubReader = EpubReader(openArchive(file))

    private fun openArchive(file: StorageFile): ArchiveReader {
        return try {
            archives.open(file)
        } catch (e: LibarchiveUnavailableException) {
            throw Exception(localize(MR.strings.local_archive_library_missing), e)
        }
    }

    companion object {
        const val ID = LOCAL_SOURCE_ID

        private val LATEST_THRESHOLD = 7.days.inWholeMilliseconds
    }
}

fun Entry.isLocal(): Boolean = source == LocalSource.ID

fun DomainSource.isLocal(): Boolean = id == LocalSource.ID

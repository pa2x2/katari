package mihon.entry.interactions.book.download

import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.util.lang.Hash
import mihon.book.api.BookCatalogCoverage
import mihon.book.api.BookContentDescriptor
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import java.io.File
import java.nio.file.Files

internal data class BookDownloadFixture(
    val root: File,
    val provider: BookDownloadProvider,
    val entry: Entry,
    val child: EntryChapter,
) {
    val packageKey = BookDownloadPackageKey(entry.source, entry.url, child.url)

    fun complete(
        content: String,
        entry: Entry = this.entry,
        child: EntryChapter = this.child,
    ): VerifiedBookDownloadPackage {
        val staging = provider.beginPackage("Fixture Source", entry, child).getOrThrow()
        val bytes = content.encodeToByteArray()
        val fileName = provider.resourceFileName("chapter", "text/html")
        staging.directory.createFile(fileName)!!.openOutputStream().use { it.write(bytes) }
        return provider.completePackage(
            staging = staging,
            manifest = manifest(
                storedSize = bytes.size.toLong(),
                sha256 = Hash.sha256(bytes),
                fileName = fileName,
                entry = entry,
                child = child,
            ),
        ).getOrThrow()
    }

    fun manifest(
        version: Int = BookDownloadManifest.CURRENT_VERSION,
        storedSize: Long,
        sha256: String,
        fileName: String = "chapter.html",
        entry: Entry = this.entry,
        child: EntryChapter = this.child,
    ) = BookDownloadManifest(
        version = version,
        sourceId = entry.source,
        entryId = entry.id,
        entryTitle = entry.title,
        entryUrl = entry.url,
        childId = child.id,
        childTitle = child.name,
        childUrl = child.url,
        descriptor = BookContentDescriptor("text/html", profile = "prose-chapter"),
        publicationId = "source:${entry.source}:entry:${entry.url}",
        publicationRevision = "publication-v1",
        catalogRevision = "catalog-v1",
        catalogCoverage = BookCatalogCoverage.PARTIAL,
        primaryResourceIds = listOf("chapter"),
        resources = listOf(
            BookDownloadedResource(
                id = "chapter",
                title = child.name,
                order = 0,
                mediaType = "text/html",
                revision = "chapter-v1",
                fileName = fileName,
                storedSize = storedSize,
                sha256 = sha256,
            ),
        ),
        createdAt = child.id,
    )
}

internal fun fixture(): BookDownloadFixture {
    val root = Files.createTempDirectory("katari-book-downloads").toFile()
    val entry = Entry.create().copy(
        id = 1L,
        source = 42L,
        url = "/book/fixture",
        title = "Fixture Book",
        type = EntryType.BOOK,
    )
    val child = EntryChapter.create().copy(
        id = 11L,
        entryId = entry.id,
        url = "/chapter/1",
        name = "Chapter 1",
    )
    return BookDownloadFixture(
        root = root,
        provider = BookDownloadProvider(downloadsDirectory = { UniFile.fromFile(root) }),
        entry = entry,
        child = child,
    )
}

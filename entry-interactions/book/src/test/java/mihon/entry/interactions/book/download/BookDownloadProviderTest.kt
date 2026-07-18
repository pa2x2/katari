package mihon.entry.interactions.book.download

import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.util.lang.Hash
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import mihon.book.api.BookCatalogCoverage
import mihon.book.api.BookContentDescriptor
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryMerge
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class BookDownloadProviderTest {
    @Test
    fun `persisted index restores without scanning and verifies lazily`() = runTest {
        val fixture = fixture()
        val completed = fixture.complete(content = "offline chapter")
        val store = BookDownloadIndexStore(
            context = RuntimeEnvironment.getApplication(),
            cacheFile = fixture.root.resolve("book-index"),
        )
        BookDownloadCache(fixture.provider, store).refresh()
        val restoredProvider = spyk(fixture.provider)
        val restoredCache = BookDownloadCache(restoredProvider, store)

        restoredCache.ensureInitialized()

        verify(exactly = 0) { restoredProvider.scanPackages() }
        verify(exactly = 0) { restoredProvider.rebuildPackages() }
        verify(exactly = 0) { restoredProvider.readVerifiedPackage(any()) }
        assertEquals(1, restoredCache.getTotalDownloadCount())

        val verified = restoredCache.getVerified(fixture.packageKey)

        assertEquals(completed.manifest, verified?.manifest)
        verify(exactly = 1) { restoredProvider.readVerifiedPackage(any()) }
    }

    @Test
    fun `failed lazy verification evicts and persists the stale package`() = runTest {
        val fixture = fixture()
        val completed = fixture.complete(content = "offline chapter")
        val store = BookDownloadIndexStore(
            context = RuntimeEnvironment.getApplication(),
            cacheFile = fixture.root.resolve("book-index"),
        )
        BookDownloadCache(fixture.provider, store).refresh()
        completed.resources.getValue("chapter").openOutputStream().use {
            it.write("tampered chapter".encodeToByteArray())
        }
        val restoredCache = BookDownloadCache(fixture.provider, store)
        restoredCache.ensureInitialized()
        assertEquals(1, restoredCache.getTotalDownloadCount())

        assertNull(restoredCache.getVerified(fixture.packageKey))

        assertEquals(0, restoredCache.getTotalDownloadCount())
        val nextCache = BookDownloadCache(spyk(fixture.provider), store)
        nextCache.ensureInitialized()
        assertEquals(0, nextCache.getTotalDownloadCount())
    }

    @Test
    fun `corrupt persisted index falls back to one full rebuild`() = runTest {
        val fixture = fixture()
        fixture.complete(content = "offline chapter")
        val indexFile = fixture.root.resolve("book-index").apply { writeText("not protobuf") }
        val provider = spyk(fixture.provider)
        val cache = BookDownloadCache(
            provider = provider,
            indexStore = BookDownloadIndexStore(RuntimeEnvironment.getApplication(), indexFile),
        )

        cache.ensureInitialized()

        verify(exactly = 1) { provider.rebuildPackages() }
        assertEquals(1, cache.getTotalDownloadCount())
    }

    @Test
    fun `incremental inserts never rescan accumulated packages`() = runTest {
        val fixture = fixture()
        val completed = fixture.complete(content = "offline chapter")
        val provider = spyk(fixture.provider)
        val cache = BookDownloadCache(provider)
        cache.ensureInitialized()
        clearMocks(provider, answers = false)

        repeat(300) { index ->
            cache.upsert(
                completed.copy(
                    manifest = completed.manifest.copy(
                        childId = 1_000L + index,
                        childTitle = "Chapter $index",
                        childUrl = "/bulk/chapter/$index",
                        createdAt = 1_000L + index,
                    ),
                ),
            )
        }

        verify(exactly = 0) { provider.scanPackages() }
        verify(exactly = 0) { provider.rebuildPackages() }
        assertEquals(301, cache.getTotalDownloadCount())
    }

    @Test
    fun `routine cache refresh does not report download initialization`() = runTest {
        val observedInitialization = mutableListOf<Boolean>()
        lateinit var cache: BookDownloadCache
        val provider = mockk<BookDownloadProvider> {
            every { rebuildPackages() } answers {
                observedInitialization += cache.isInitializing.value
                BookDownloadPackageScan(emptyList(), 0)
            }
        }
        cache = BookDownloadCache(provider)

        cache.refresh()
        cache.refresh(reportInitialization = true)

        assertEquals(listOf(false, true), observedInitialization)
    }

    @Test
    fun `completed package is verified and indexed`() = runTest {
        val fixture = fixture()
        val completed = fixture.complete(content = "offline chapter")
        val cache = BookDownloadCache(fixture.provider)

        val refresh = cache.refresh()

        assertEquals(1, refresh.packageCount)
        assertEquals(0, refresh.invalidPackageCount)
        assertEquals(completed.manifest, cache.get(fixture.packageKey)?.manifest)
        assertTrue(cache.isDownloaded(fixture.packageKey))
        assertEquals(1, cache.getDownloadCount(fixture.entry.source, fixture.entry.url))
        assertEquals(1, cache.getTotalDownloadCount())
    }

    @Test
    fun `partial and corrupt packages never become downloaded`() = runTest {
        val fixture = fixture()
        val staging = fixture.provider.beginPackage("Fixture Source", fixture.entry, fixture.child).getOrThrow()
        staging.directory.createFile("partial.html")!!.openOutputStream().use {
            it.write("partial".encodeToByteArray())
        }
        val valid = fixture.complete(
            content = "valid chapter",
            child = fixture.child.copy(id = 12L, url = "/chapter/2"),
        )
        valid.resources.getValue("chapter").openOutputStream().use { it.write("tampered".encodeToByteArray()) }
        val cache = BookDownloadCache(fixture.provider)

        val refresh = cache.refresh()

        assertEquals(0, refresh.packageCount)
        assertEquals(1, refresh.invalidPackageCount)
        assertEquals(1, refresh.cleanedTemporaryPackageCount)
        assertFalse(staging.directory.exists())
    }

    @Test
    fun `cleanup restores preserved package when publication was interrupted`() = runTest {
        val fixture = fixture()
        val completed = fixture.complete(content = "original")
        val originalName = assertNotNull(completed.directory.name)
        assertTrue(completed.directory.renameTo(originalName + BookDownloadProvider.BACKUP_SUFFIX))

        val scan = fixture.provider.rebuildPackages()

        assertEquals(1, scan.cleanedTemporaryPackageCount)
        assertEquals(1, scan.packages.size)
        assertEquals(
            "original",
            scan.packages.single().resources.getValue("chapter").openInputStream().reader().readText(),
        )
    }

    @Test
    fun `manifest rejects unsafe or unsupported package data`() {
        val fixture = fixture()

        assertFailsWith<IllegalArgumentException> {
            fixture.manifest(
                storedSize = 1,
                sha256 = Hash.sha256("x"),
                fileName = "../chapter.html",
            )
        }
        assertFailsWith<IllegalArgumentException> {
            fixture.manifest(
                version = BookDownloadManifest.CURRENT_VERSION + 1,
                storedSize = 1,
                sha256 = Hash.sha256("x"),
            )
        }
    }

    @Test
    fun `failed replacement leaves the completed package readable`() {
        val fixture = fixture()
        fixture.complete(content = "original")
        val staging = fixture.provider.beginPackage("Fixture Source", fixture.entry, fixture.child).getOrThrow()
        val replacement = "replacement".encodeToByteArray()
        val fileName = fixture.provider.resourceFileName("chapter", "text/html")
        staging.directory.createFile(fileName)!!.openOutputStream().use { it.write(replacement) }
        val invalidManifest = fixture.manifest(
            storedSize = replacement.size.toLong(),
            sha256 = Hash.sha256("different bytes"),
            fileName = fileName,
        )

        assertTrue(fixture.provider.completePackage(staging, invalidManifest).isFailure)
        val completed = fixture.provider.scanPackages().packages.single()
        assertEquals(
            "original",
            completed.resources.getValue("chapter").openInputStream().reader().use { it.readText() },
        )
    }

    @Test
    fun `source and entry directory renames keep packages discoverable`() {
        val fixture = fixture()
        fixture.complete(content = "offline")

        assertTrue(fixture.provider.renameSource("Fixture Source", "Renamed Source"))
        assertTrue(fixture.provider.renameEntry("Renamed Source", fixture.entry, "Renamed Book"))

        val completed = fixture.provider.scanPackages().packages.single()
        assertEquals(fixture.packageKey, completed.manifest.packageKey)
        assertEquals(
            "offline",
            completed.resources.getValue("chapter").openInputStream().reader().use {
                it.readText()
            },
        )
    }

    @Test
    fun `merged download count includes packages owned by every member`() = runTest {
        val fixture = fixture()
        val member = fixture.entry.copy(id = 2L, url = "/book/member", title = "Member Book")
        val memberChild = fixture.child.copy(id = 21L, entryId = member.id, url = "/member/chapter/1")
        fixture.complete(content = "target")
        fixture.complete(content = "member", entry = member, child = memberChild)
        val cache = BookDownloadCache(fixture.provider)
        cache.refresh()
        cache.updateMergedEntries(
            listOf(
                EntryMerge(targetId = fixture.entry.id, entryId = fixture.entry.id, position = 0L),
                EntryMerge(targetId = fixture.entry.id, entryId = member.id, position = 1L),
            ),
        )

        assertEquals(2, cache.getDownloadCount(fixture.entry))
        assertEquals(2, cache.getDownloadCount(member))
    }
}

private data class BookDownloadFixture(
    val root: java.io.File,
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

private fun fixture(): BookDownloadFixture {
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

package mihon.entry.interactions.book.download

import eu.kanade.tachiyomi.util.lang.Hash
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class BookDownloadProviderTest {
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
}

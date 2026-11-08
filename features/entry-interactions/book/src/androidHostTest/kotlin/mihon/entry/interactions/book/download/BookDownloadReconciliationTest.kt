package mihon.entry.interactions.book.download

import com.hippo.unifile.UniFile
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.storage.ListedStorageFile
import tachiyomi.core.common.storage.StorageFile
import tachiyomi.core.common.storage.UniFileStorageFile
import java.io.File
import java.net.URI
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class BookDownloadReconciliationTest {
    @Test
    fun `package listing detects additions and removals without an entry timestamp change`() = runTest {
        val fixture = fixture()
        val removed = fixture.complete(content = "removed chapter")
        val stableChild = fixture.child.copy(id = 13L, url = "/chapter/3", name = "Chapter 3")
        val stable = fixture.complete(content = "stable chapter", child = stableChild)
        val entryDirectory = checkNotNull(File(URI(stable.directory.uri)).parentFile)
        val initialEntryTimestamp = entryDirectory.lastModified()
        val listings = mutableListOf<String>()
        val provider = fixture.reconciliationProvider(listings = listings)
        provider.discoverPackages()
        listings.clear()
        val addedChild = fixture.child.copy(id = 12L, url = "/chapter/2", name = "Chapter 2")
        val added = fixture.complete(content = "added chapter", child = addedChild)
        assertTrue(removed.directory.delete())
        assertTrue(entryDirectory.setLastModified(initialEntryTimestamp))

        val scan = provider.discoverPackages()

        assertEquals(
            setOf(stable.manifest.packageKey, added.manifest.packageKey),
            scan.packages.mapTo(mutableSetOf()) { it.manifest.packageKey },
        )
        assertEquals(0, listings.count { it == stable.directory.uri })
        assertEquals(1, listings.count { it == added.directory.uri })
    }

    @Test
    fun `corrupt private summary falls back to package manifests`() = runTest {
        val fixture = fixture()
        val completed = fixture.complete(content = "offline chapter")
        val reconciliationFile = fixture.root.resolve("reconciliation")
        val provider = fixture.reconciliationProvider(reconciliationFile)
        provider.discoverPackages()
        reconciliationFile.writeText("not protobuf")

        val scan = provider.discoverPackages()

        assertEquals(listOf(completed.manifest.packageKey), scan.packages.map { it.manifest.packageKey })
    }

    @Test
    fun `summary from another storage root is never reused`() = runTest {
        val original = fixture()
        original.complete(content = "original root")
        val reconciliationFile = original.root.resolve("reconciliation")
        original.reconciliationProvider(reconciliationFile).discoverPackages()
        val moved = fixture()
        val movedPackage = moved.complete(content = "moved root")
        val listings = mutableListOf<String>()
        val provider = moved.reconciliationProvider(reconciliationFile, listings)

        val scan = provider.discoverPackages()

        assertEquals(listOf(movedPackage.manifest.packageKey), scan.packages.map { it.manifest.packageKey })
        assertEquals(1, listings.count { it == movedPackage.directory.uri })
    }

    private fun BookDownloadFixture.reconciliationProvider(
        reconciliationFile: File = root.resolve("reconciliation"),
        listings: MutableList<String> = mutableListOf(),
    ) = BookDownloadProvider(
        downloadsDirectory = { ListingRecordingFile(UniFileStorageFile(UniFile.fromFile(root)!!), listings) },
        reconciliationStore = BookDownloadReconciliationStore(reconciliationFile),
    )
}

/** Records the URI of every directory listed through it or the files reached from it. */
private class ListingRecordingFile(
    private val delegate: StorageFile,
    private val listings: MutableList<String>,
) : StorageFile by delegate {
    override fun listFiles(): List<StorageFile>? = delegate.listFiles()?.map(::recording)

    override fun listWithDetails(): List<ListedStorageFile> {
        listings += uri
        return delegate.listWithDetails().map { it.copy(file = recording(it.file)) }
    }

    override fun findFile(name: String): StorageFile? = delegate.findFile(name)?.let(::recording)

    override fun createFile(name: String): StorageFile? = delegate.createFile(name)?.let(::recording)

    override fun createDirectory(name: String): StorageFile? = delegate.createDirectory(name)?.let(::recording)

    override fun renameTo(name: String): StorageFile? = delegate.renameTo(name)?.let(::recording)

    private fun recording(file: StorageFile) = ListingRecordingFile(file, listings)
}

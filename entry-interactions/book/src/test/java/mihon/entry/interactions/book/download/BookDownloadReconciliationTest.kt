package mihon.entry.interactions.book.download

import com.hippo.unifile.UniFile
import io.mockk.clearMocks
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
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
        val entryDirectory = File(checkNotNull(stable.directory.parentFile?.filePath))
        val initialEntryTimestamp = entryDirectory.lastModified()
        val directoryListing = spyk(BookDownloadDirectoryListing())
        val provider = fixture.reconciliationProvider(directoryListing = directoryListing)
        provider.discoverPackages()
        clearMocks(directoryListing, answers = false)
        val addedChild = fixture.child.copy(id = 12L, url = "/chapter/2", name = "Chapter 2")
        val added = fixture.complete(content = "added chapter", child = addedChild)
        assertTrue(removed.directory.delete())
        assertTrue(entryDirectory.setLastModified(initialEntryTimestamp))

        val scan = provider.discoverPackages()

        assertEquals(
            setOf(stable.manifest.packageKey, added.manifest.packageKey),
            scan.packages.mapTo(mutableSetOf()) { it.manifest.packageKey },
        )
        verify(exactly = 0) { directoryListing.list(match { it.uri == stable.directory.uri }) }
        verify(exactly = 1) { directoryListing.list(match { it.uri == added.directory.uri }) }
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
        val directoryListing = spyk(BookDownloadDirectoryListing())
        val provider = BookDownloadProvider(
            downloadsDirectory = { UniFile.fromFile(moved.root) },
            directoryListing = directoryListing,
            reconciliationStore = BookDownloadReconciliationStore(reconciliationFile),
        )

        val scan = provider.discoverPackages()

        assertEquals(listOf(movedPackage.manifest.packageKey), scan.packages.map { it.manifest.packageKey })
        verify(exactly = 1) { directoryListing.list(match { it.uri == movedPackage.directory.uri }) }
    }

    private fun BookDownloadFixture.reconciliationProvider(
        reconciliationFile: File = root.resolve("reconciliation"),
        directoryListing: BookDownloadDirectoryListing = BookDownloadDirectoryListing(),
    ) = BookDownloadProvider(
        downloadsDirectory = { UniFile.fromFile(root) },
        directoryListing = directoryListing,
        reconciliationStore = BookDownloadReconciliationStore(reconciliationFile),
    )
}

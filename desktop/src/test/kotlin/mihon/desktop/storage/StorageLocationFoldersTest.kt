package mihon.desktop.storage

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.core.common.storage.FolderProvider
import tachiyomi.core.common.storage.PathStorageFile
import tachiyomi.domain.storage.service.StoragePreferences
import java.io.File
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.listDirectoryEntries

class StorageLocationFoldersTest {

    @TempDir
    lateinit var home: Path

    @Test
    fun `nothing goes into an existing default folder before the user picks a storage location`() {
        val default = home.resolve("Katari").createDirectories()
        val picked = home.resolve("Picked").createDirectories()
        val preferences = StoragePreferences(
            folderProvider = object : FolderProvider {
                override fun directory(): File = default.toFile()
                override fun path(): String = default.toUri().toString()
            },
            preferenceStore = InMemoryPreferenceStore(),
        )
        val folders = StorageLocationFolders(preferences)

        folders.automaticBackups().shouldBeNull()
        folders.localSource().shouldBeNull()
        default.listDirectoryEntries() shouldBe emptyList()

        preferences.baseStorageDirectory.set(picked.toUri().toString())

        (folders.automaticBackups() as PathStorageFile).path shouldBe picked.resolve("autobackup")
        default.listDirectoryEntries() shouldBe emptyList()
    }
}

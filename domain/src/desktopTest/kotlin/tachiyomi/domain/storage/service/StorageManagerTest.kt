package tachiyomi.domain.storage.service

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.core.common.storage.FolderProvider
import tachiyomi.core.common.storage.NoMediaScanner
import tachiyomi.core.common.storage.PathStorageFileResolver
import tachiyomi.core.common.storage.toPath
import java.io.File
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.listDirectoryEntries

class StorageManagerTest {

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
        val storage = StorageManager(PathStorageFileResolver(), NoMediaScanner, preferences, TestScope())

        storage.getAutomaticBackupsDirectory().shouldBeNull()
        storage.getLocalSourceDirectory().shouldBeNull()
        storage.getDownloadsDirectory().shouldBeNull()
        default.listDirectoryEntries() shouldBe emptyList()

        preferences.baseStorageDirectory.set(picked.toUri().toString())

        storage.getAutomaticBackupsDirectory()?.toPath() shouldBe picked.resolve("autobackup")
        default.listDirectoryEntries() shouldBe emptyList()
    }
}

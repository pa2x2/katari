package app.katari.sync.client

import app.katari.sync.client.file.local.FileOwner
import app.katari.sync.client.file.local.LocalFileStore
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.cache.CoverCacheDirectories
import kotlinx.coroutines.CoroutineScope
import mihon.core.platform.storage.AppPrivateDirectories
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.CopyOnWriteArrayList

/**
 * A device's files as the app keeps them: custom covers in the app's own cover cache, imported fonts one folder per
 * profile like the book reader's font store, each reporting every change as the app's do. [setCover] and the other
 * writes stand for the person changing them on a screen.
 */
internal class TestFileStore(directory: File, scope: CoroutineScope) : LocalFileStore {
    private val fontsDirectory = directory.resolve("fonts")
    private val fontWatchers = CopyOnWriteArrayList<(FileOwner) -> Unit>()

    val coverCache = CoverCache(
        scope,
        object : CoverCacheDirectories {
            override val covers = directory.resolve("covers").apply { mkdirs() }
            override val customCovers = directory.resolve("covers/custom").apply { mkdirs() }
        },
    )

    val directories = object : AppPrivateDirectories {
        override val cache = directory.resolve("cache").apply { mkdirs() }
        override val files = directory.resolve("files").apply { mkdirs() }
        override val noBackupFiles = directory.resolve("no-backup").apply { mkdirs() }
    }

    fun setCover(entryId: Long, bytes: ByteArray) = coverCache.setCustomCoverToCache(entryId, bytes.inputStream())

    fun cover(entryId: Long): ByteArray? = file(FileOwner.CustomCover(entryId))?.readBytes()

    fun importFont(profileId: Long, fileName: String, bytes: ByteArray) {
        val source = File.createTempFile("font", ".tmp").apply { writeBytes(bytes) }
        put(FileOwner.Font(profileId, fileName), source)
        source.delete()
    }

    override fun file(owner: FileOwner): File? = when (owner) {
        is FileOwner.CustomCover -> coverCache.getCustomCoverFile(owner.entryId).takeIf { it.isFile }
        is FileOwner.Font -> fontFile(owner).takeIf { it.isFile }
    }

    override fun put(owner: FileOwner, source: File) {
        when (owner) {
            is FileOwner.CustomCover -> source.inputStream().use {
                coverCache.replaceCustomCover(owner.entryId, it, temporarySuffix = "test")
            }
            is FileOwner.Font -> {
                val target = fontFile(owner).apply { parentFile.mkdirs() }
                val pending = File(target.parentFile, "${target.name}.tmp")
                source.copyTo(pending, overwrite = true)
                Files.move(pending.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE)
                fontWatchers.forEach { it(owner) }
            }
        }
    }

    override fun remove(owner: FileOwner) {
        when (owner) {
            is FileOwner.CustomCover -> coverCache.deleteCustomCover(owner.entryId)
            is FileOwner.Font -> if (fontFile(owner).delete()) fontWatchers.forEach { it(owner) }
        }
    }

    override fun fonts(profileId: Long): List<String> =
        fontsDirectory.resolve("$profileId").listFiles { file -> file.extension == "ttf" }.orEmpty().map { it.name }

    override fun watch(onChange: (FileOwner) -> Unit): AutoCloseable {
        val covers = coverCache.watchCustomCovers { onChange(FileOwner.CustomCover(it)) }
        fontWatchers += onChange
        return AutoCloseable {
            covers.close()
            fontWatchers -= onChange
        }
    }

    private fun fontFile(owner: FileOwner.Font) = fontsDirectory.resolve("${owner.profileId}/${owner.fileName}")
}

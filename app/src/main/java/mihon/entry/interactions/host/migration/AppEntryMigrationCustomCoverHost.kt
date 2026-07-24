package mihon.entry.interactions.host

import android.content.Context
import android.system.Os
import eu.kanade.tachiyomi.data.cache.CoverCache
import tachiyomi.domain.entry.model.Entry
import java.io.File

internal class AppEntryMigrationCustomCoverHost(
    context: Context,
    private val coverCache: CoverCache,
) : EntryMigrationCustomCoverHost {
    private val stageDirectory = File(context.cacheDir, "entry-migration/custom-covers")

    override suspend fun stage(
        operationId: String,
        source: Entry,
        target: Entry,
    ): EntryMigrationCustomCoverPayload? {
        val sourceFile = coverCache.getCustomCoverFile(source.id)
        if (!sourceFile.exists()) return null
        val stageId = operationId.requireSafeStageId()
        stageDirectory.mkdirs()
        val staged = stagedFile(stageId)
        val temporary = File(stageDirectory, "$stageId.tmp")
        sourceFile.inputStream().use { input ->
            temporary.outputStream().use(input::copyTo)
        }
        check(temporary.renameTo(staged) || (staged.exists() && temporary.delete())) {
            "Unable to persist staged Migration cover"
        }
        return EntryMigrationCustomCoverPayload(stageId, target.id)
    }

    override suspend fun promote(payload: EntryMigrationCustomCoverPayload) {
        val staged = stagedFile(payload.stageId.requireSafeStageId())
        check(staged.exists()) { "Staged Migration cover is missing" }
        val target = coverCache.getCustomCoverFile(payload.targetEntryId)
        val temporary = File(target.parentFile, "${target.name}.${payload.stageId}.tmp")
        try {
            staged.inputStream().use { input -> temporary.outputStream().use(input::copyTo) }
            Os.rename(temporary.absolutePath, target.absolutePath)
        } finally {
            temporary.delete()
        }
    }

    override suspend fun discard(payload: EntryMigrationCustomCoverPayload) {
        val staged = stagedFile(payload.stageId.requireSafeStageId())
        check(!staged.exists() || staged.delete()) { "Unable to discard staged Migration cover" }
    }

    override suspend fun cleanupOrphans(
        activeStageIds: Set<String>,
        olderThanMillis: Long,
        limit: Int,
    ) {
        require(limit > 0) { "Migration cover cleanup limit must be positive" }
        stageDirectory.listFiles()
            .orEmpty()
            .asSequence()
            .filter { file -> file.isFile && file.extension == "cover" }
            .filter { file -> file.nameWithoutExtension !in activeStageIds && file.lastModified() < olderThanMillis }
            .sortedBy(File::lastModified)
            .take(limit)
            .forEach(File::delete)
    }

    private fun stagedFile(stageId: String) = File(stageDirectory, "$stageId.cover")
}

private fun String.requireSafeStageId(): String {
    require(matches(Regex("[A-Za-z0-9-]+"))) { "Invalid Migration cover stage ID" }
    return this
}

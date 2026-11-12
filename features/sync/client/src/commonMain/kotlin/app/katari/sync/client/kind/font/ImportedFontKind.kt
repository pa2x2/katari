package app.katari.sync.client.kind.font

import app.katari.sync.client.apply.ApplyOutcome
import app.katari.sync.client.apply.LocalWrites
import app.katari.sync.client.apply.idOr
import app.katari.sync.client.capture.CaptureTrigger
import app.katari.sync.client.file.local.FileOwner
import app.katari.sync.client.file.local.want
import app.katari.sync.client.file.local.wanted
import app.katari.sync.client.identity.RecordKeys
import app.katari.sync.client.kind.profile.ProfileKind
import app.katari.sync.client.merge.latest
import app.katari.sync.client.part.DataGroup
import app.katari.sync.client.record.LocalRead
import app.katari.sync.client.record.LocalReads
import app.katari.sync.client.record.LocalRef
import app.katari.sync.client.record.RecordRef
import app.katari.sync.client.record.SyncedKind
import app.katari.sync.protocol.file.FileCategory
import app.katari.sync.protocol.file.FileReference

/**
 * Fonts imported for the book reader, keyed by profile uuid and file name. Local ref: `[profile_id, file_name]`
 * ([FileOwner.Font]). Fonts are files, not rows, so no trigger captures them: importing or deleting one is heard from
 * the app's file store ([app.katari.sync.client.file.capture.FileCapture]). Deleting a font deletes the record, which
 * releases its file on the server, but deleting its profile leaves the record to the profile's deletion, as the
 * profile's rows are; two devices importing different files under one name keep the later.
 */
object ImportedFontKind : SyncedKind<ImportedFontPayload>(
    "font.imported",
    1,
    ImportedFontPayload.serializer(),
    Absence.TOMBSTONE,
) {
    override val dataGroup = DataGroup.FONTS

    override fun profileOf(payload: ImportedFontPayload) = payload.profileUuid

    override val capture = emptyList<CaptureTrigger>()

    fun ref(profileId: Long, fileName: String): LocalRef = FileOwner.Font(profileId, fileName).ref

    override suspend fun read(reads: LocalReads, ref: LocalRef): LocalRead<ImportedFontPayload> {
        val owner = FileOwner.of(FileCategory.FONT, ref) as FileOwner.Font
        val profileUuid = reads.profileUuid(owner.profileId) ?: return LocalRead.Missing
        val file = reads.database.wanted(owner)?.file ?: return LocalRead.Missing
        return LocalRead.Found(
            key = RecordKeys.derive(name, profileUuid, owner.fileName),
            parents = listOf(RecordRef(ProfileKind, ProfileKind.ref(profileUuid))),
        ) { previous, stamps ->
            ImportedFontPayload(profileUuid, owner.fileName, stamps.field(3, file, previous?.file))
        }
    }

    override fun merge(local: ImportedFontPayload, incoming: ImportedFontPayload) =
        local.copy(file = latest(local.file, incoming.file))

    override fun files(payload: ImportedFontPayload): List<FileReference> =
        listOf(payload.file.value.reference(FileCategory.FONT))

    /**
     * Names the font's file for the profile; it is put in place after the transaction. A name that isn't a plain
     * font file name, which the app never makes, is not written anywhere.
     */
    override suspend fun apply(writes: LocalWrites, key: String, payload: ImportedFontPayload): ApplyOutcome {
        val profileId = writes.profileId(payload.profileUuid).idOr { return it }
        if (!isFontFileName(payload.fileName)) return ApplyOutcome.Applied(null)
        val owner = FileOwner.Font(profileId, payload.fileName)
        writes.database.want(owner, payload.file.value)
        return ApplyOutcome.Applied(owner.ref)
    }

    override suspend fun applyDeletion(writes: LocalWrites, key: String, ref: LocalRef?): ApplyOutcome {
        ref?.let { writes.database.want(FileOwner.of(FileCategory.FONT, it), null) }
        return ApplyOutcome.Applied(null)
    }

    private fun isFontFileName(name: String): Boolean = name.isNotBlank() &&
        name.none { it == '/' || it == '\\' || it.code < 0x20 } &&
        name != "." && name != ".." &&
        name.substringAfterLast('.', "").lowercase() in FONT_EXTENSIONS

    private val FONT_EXTENSIONS = setOf("ttf", "otf")
}

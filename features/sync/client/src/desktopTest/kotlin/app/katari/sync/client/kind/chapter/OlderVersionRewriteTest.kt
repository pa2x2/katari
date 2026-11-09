package app.katari.sync.client.kind.chapter

import app.katari.sync.client.payload.Stamped
import app.katari.sync.client.payload.UnknownFields
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.ProtoNumber
import org.junit.jupiter.api.Test

class OlderVersionRewriteTest {

    @Test
    fun `a device on the first release with sync keeps when a chapter was found and its place`() {
        val written = ChapterKind.encode(
            ChapterPayload(
                entryKey = "entry",
                url = "/show/3",
                read = Stamped(false, Stamped.UNKNOWN_EDIT),
                bookmark = Stamped(false, Stamped.UNKNOWN_EDIT),
                metadata = ChapterMetadata(name = "Episode 3", number = 3.0),
                dateFetch = 2000,
                sourceOrder = 0,
            ),
            previous = null,
        )

        // As that release rewrites the record when the person reads the chapter there
        val older = ProtoBuf.decodeFromByteArray(FirstReleaseChapterPayload.serializer(), written)
        val rewritten = ProtoBuf.encodeToByteArray(
            FirstReleaseChapterPayload.serializer(),
            older.copy(read = Stamped(true, Stamped.UNKNOWN_EDIT)),
        ) + UnknownFields.of(written, UnknownFields.numbersOf(FirstReleaseChapterPayload.serializer().descriptor))

        val payload = ChapterKind.decode(rewritten)
        payload.read.value shouldBe true
        payload.dateFetch shouldBe 2000
        payload.sourceOrder shouldBe 0
    }
}

/** [ChapterPayload] as the first release with sync (3.0.0) has it. */
@Serializable
private data class FirstReleaseChapterPayload(
    @ProtoNumber(1) val entryKey: String,
    @ProtoNumber(2) val url: String,
    @ProtoNumber(3) val read: Stamped<Boolean>,
    @ProtoNumber(4) val bookmark: Stamped<Boolean>,
    @ProtoNumber(5) val metadata: FirstReleaseChapterMetadata,
)

@Serializable
private data class FirstReleaseChapterMetadata(
    @ProtoNumber(1) val name: String,
    @ProtoNumber(2) val number: Double,
    @ProtoNumber(3) val scanlator: String? = null,
    @ProtoNumber(4) val dateUpload: Long = 0,
)

package tachiyomi.domain.entry.model

import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.EntryUpdateStrategy
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class EntrySerializationTest {

    @Test
    fun `java serialization preserves every entry field`() {
        val entry = Entry.create().copy(
            id = 1,
            source = 2,
            favorite = true,
            lastUpdate = 3,
            nextUpdate = 4,
            fetchInterval = 5,
            dateAdded = 6,
            viewerFlags = 7,
            chapterFlags = 8,
            coverLastModified = 9,
            url = "url",
            title = "title",
            displayName = "display name",
            artist = "artist",
            author = "author",
            description = "description",
            genre = listOf("genre"),
            status = EntryStatus.ON_HIATUS,
            thumbnailUrl = "thumbnail",
            updateStrategy = EntryUpdateStrategy.ONLY_FETCH_ONCE,
            initialized = true,
            lastModifiedAt = 10,
            favoriteModifiedAt = 11,
            version = 12,
            notes = "notes",
            memo = buildJsonObject { put("key", JsonPrimitive("value")) },
            isSyncing = true,
            type = EntryType.ANIME,
            profileId = 13,
        )

        val bytes = ByteArrayOutputStream().use { bytes ->
            ObjectOutputStream(bytes).use { it.writeObject(entry) }
            bytes.toByteArray()
        }
        val restored = ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() }

        restored shouldBe entry
    }
}

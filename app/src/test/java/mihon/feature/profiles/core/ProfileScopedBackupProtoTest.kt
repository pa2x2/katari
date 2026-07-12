package mihon.feature.profiles.core

import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.ProtoNumber
import org.junit.jupiter.api.Test

class ProfileScopedBackupProtoTest {

    @Test
    fun `legacy profile backup bytes decode without type`() {
        val bytes = ProtoBuf.encodeToByteArray(
            serializer = LegacyProfileBackup.serializer(),
            LegacyProfileBackup(
                uuid = "legacy-uuid",
                name = "Legacy",
                colorSeed = 123L,
                position = 4L,
                requiresAuth = true,
                isArchived = false,
            ),
        )

        ProtoBuf.decodeFromByteArray(ProfileBackup.serializer(), bytes) shouldBe ProfileBackup(
            uuid = "legacy-uuid",
            name = "Legacy",
            colorSeed = 123L,
            position = 4L,
            requiresAuth = true,
            isArchived = false,
        )
    }

    @Test
    fun `legacy scoped backup bytes decode without type`() {
        val bytes = ProtoBuf.encodeToByteArray(
            serializer = LegacyProfileScopedBackup.serializer(),
            LegacyProfileScopedBackup(
                profile = LegacyProfileBackup(
                    uuid = "legacy-uuid",
                    name = "Legacy",
                    colorSeed = 123L,
                    position = 4L,
                    requiresAuth = true,
                    isArchived = false,
                ),
            ),
        )

        ProtoBuf.decodeFromByteArray(ProfileScopedBackup.serializer(), bytes) shouldBe ProfileScopedBackup(
            profile = ProfileBackup(
                uuid = "legacy-uuid",
                name = "Legacy",
                colorSeed = 123L,
                position = 4L,
                requiresAuth = true,
                isArchived = false,
            ),
        )
    }

    @Test
    fun `legacy profile backup with type decodes as untyped profile`() {
        val bytes = ProtoBuf.encodeToByteArray(
            serializer = LegacyProfileBackupWithType.serializer(),
            LegacyProfileBackupWithType(
                uuid = "legacy-uuid",
                name = "Legacy",
                colorSeed = 123L,
                position = 4L,
                requiresAuth = true,
                isArchived = false,
                type = ProfileType.ANIME,
            ),
        )

        ProtoBuf.decodeFromByteArray(ProfileBackup.serializer(), bytes) shouldBe ProfileBackup(
            uuid = "legacy-uuid",
            name = "Legacy",
            colorSeed = 123L,
            position = 4L,
            requiresAuth = true,
            isArchived = false,
            type = ProfileType.ANIME,
        )
    }

    @Serializable
    private data class LegacyProfileScopedBackup(
        @ProtoNumber(1) val profile: LegacyProfileBackup,
    )

    @Serializable
    private data class LegacyProfileBackup(
        @ProtoNumber(1) val uuid: String,
        @ProtoNumber(2) val name: String,
        @ProtoNumber(3) val colorSeed: Long,
        @ProtoNumber(4) val position: Long,
        @ProtoNumber(5) val requiresAuth: Boolean,
        @ProtoNumber(6) val isArchived: Boolean,
    )

    @Serializable
    private data class LegacyProfileBackupWithType(
        @ProtoNumber(1) val uuid: String,
        @ProtoNumber(2) val name: String,
        @ProtoNumber(3) val colorSeed: Long,
        @ProtoNumber(4) val position: Long,
        @ProtoNumber(5) val requiresAuth: Boolean,
        @ProtoNumber(6) val isArchived: Boolean,
        @ProtoNumber(7) val type: ProfileType,
    )
}

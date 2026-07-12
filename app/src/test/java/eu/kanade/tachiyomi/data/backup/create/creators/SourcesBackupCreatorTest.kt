package eu.kanade.tachiyomi.data.backup.create.creators

import eu.kanade.tachiyomi.data.backup.models.BackupEntry
import eu.kanade.tachiyomi.data.backup.models.BackupSource
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import tachiyomi.domain.source.service.SourceManager

class SourcesBackupCreatorTest {

    private val sourceManager = mockk<SourceManager>()
    private val creator = SourcesBackupCreator(sourceManager)

    @Test
    fun `creates source metadata from unified source without legacy conversion`() {
        every { sourceManager.getOrStub(42L) } returns source(42L, "Mixed Source")

        creator(listOf(BackupEntry(source = 42L, url = "/entry"))) shouldBe listOf(
            BackupSource(name = "Mixed Source", sourceId = 42L),
        )
    }

    private fun source(id: Long, name: String): UnifiedSource {
        val source = mockk<UnifiedSource>()
        every { source.id } returns id
        every { source.name } returns name
        return source
    }
}

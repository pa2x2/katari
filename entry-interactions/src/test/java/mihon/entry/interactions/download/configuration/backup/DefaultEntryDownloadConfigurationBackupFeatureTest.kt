package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.validation.productionSubjectEvaluation
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.DownloadPreferences
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.VideoDownloadQualityMode
import tachiyomi.domain.entry.repository.DownloadPreferencesRepository

class DefaultEntryDownloadConfigurationBackupFeatureTest {

    @Test
    fun `Download Configuration owns portable snapshot and restore mapping`() = runTest {
        val processor = mockk<EntryDownloadOptionsProcessor> {
            every { type } returns EntryType.ANIME
        }
        val repository = mockk<DownloadPreferencesRepository>()
        val entry = Entry.create().copy(id = 12, type = EntryType.ANIME)
        coEvery { repository.getByEntryId(entry.id) } returns DownloadPreferences(
            entryId = entry.id,
            dubKey = "dub",
            streamKey = "stream",
            subtitleKey = "subtitle",
            qualityMode = VideoDownloadQualityMode.DATA_SAVING,
            updatedAt = 42,
        )
        coEvery { repository.upsert(any()) } returns Unit
        val feature = DefaultEntryDownloadConfigurationBackupFeature(
            evaluation = productionSubjectEvaluation(
                EntryDownloadOptionsCapability.bind(processor),
                EntryDownloadConfigurationFeatureContributor,
            ),
            repository = repository,
        )

        val state = feature.snapshot(entry)!!
        state shouldBe EntryDownloadConfigurationBackupState(
            dubKey = "dub",
            streamKey = "stream",
            subtitleKey = "subtitle",
            qualityMode = EntryDownloadConfigurationQualityMode.DATA_SAVING,
            updatedAt = 42,
        )

        feature.restore(entry, state.copy(qualityMode = EntryDownloadConfigurationQualityMode.BEST))

        coVerify {
            repository.upsert(
                DownloadPreferences(
                    entryId = entry.id,
                    dubKey = "dub",
                    streamKey = "stream",
                    subtitleKey = "subtitle",
                    qualityMode = VideoDownloadQualityMode.BEST,
                    updatedAt = 42,
                ),
            )
        }
    }
}

package mihon.feature.migration.dialog

import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mihon.entry.interactions.EntryMigrationFeature
import mihon.entry.interactions.EntryMigrationOption
import mihon.entry.interactions.EntryMigrationPreparationResult
import mihon.entry.interactions.EntryMigrationReference
import mihon.entry.interactions.EntryMigrationSubject
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.entry.model.Entry

class MigrateEntryDialogScreenModelTest {

    @Test
    fun `successful preparation stops loading and exposes migration options`() = runTest {
        val source = entry(id = 1L, sourceId = 10L, favorite = true)
        val target = entry(id = 2L, sourceId = 20L, favorite = false)
        val reference = mockk<EntryMigrationReference>()
        val migration = mockk<EntryMigrationFeature> {
            coEvery { prepare(any()) } returns EntryMigrationPreparationResult.Ready(
                reference = reference,
                source = EntryMigrationSubject(source.profileId, source.id),
                target = EntryMigrationSubject(target.profileId, target.id),
                availableOptions = setOf(EntryMigrationOption.CHILD_STATE),
            )
        }
        val screenModel = MigrateEntryDialogScreenModel(
            sourcePreference = SourcePreferences(InMemoryPreferenceStore(), Json),
            migration = migration,
        )

        screenModel.init(source, target)

        val state = screenModel.state.value
        state.isLoading shouldBe false
        state.reference shouldBe reference
        state.availableOptions shouldBe listOf(EntryMigrationOption.CHILD_STATE)
        state.selectedOptions shouldBe setOf(EntryMigrationOption.CHILD_STATE)
    }

    private fun entry(id: Long, sourceId: Long, favorite: Boolean): Entry {
        return Entry.create().copy(
            id = id,
            profileId = 1L,
            source = sourceId,
            url = "entry-$id",
            title = "Entry $id",
            favorite = favorite,
            type = EntryType.MANGA,
        )
    }
}

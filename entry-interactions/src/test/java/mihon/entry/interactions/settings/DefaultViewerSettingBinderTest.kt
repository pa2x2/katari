package mihon.entry.interactions.settings

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import mihon.entry.viewer.settings.ViewerSettingCodecs
import mihon.entry.viewer.settings.ViewerSettingDefinition
import mihon.entry.viewer.settings.ViewerSettingId
import mihon.entry.viewer.settings.ViewerSettingOverride
import mihon.entry.viewer.settings.ViewerSettingOverrideRepository
import mihon.entry.viewer.settings.ViewerSettingScope
import mihon.entry.viewer.settings.ViewerSettingSource
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore

class DefaultViewerSettingBinderTest {

    private val preference = InMemoryPreferenceStore().getInt("mode", 2)
    private val definition = ViewerSettingDefinition(
        id = ViewerSettingId("test.reader", "mode"),
        scope = ViewerSettingScope.PROFILE_WITH_ENTRY_OVERRIDE,
        processorDefault = 2,
        profilePreference = preference,
        codec = ViewerSettingCodecs.Int,
        validate = { it in 1..3 },
    )

    @Test
    fun `invalid layers are preserved but ignored`() = runTest {
        preference.set(99)
        val repository = FakeOverrideRepository()
        repository.upsert(ViewerSettingOverride(7, definition.id, "not-an-int", 5))

        DefaultViewerSettingBinder(repository, backgroundScope).resolve(definition, entryId = 7).run {
            effectiveValue shouldBe 2
            source shouldBe ViewerSettingSource.PROCESSOR_DEFAULT
            invalidProfileValue shouldBe true
            invalidEntryOverride shouldBe true
        }
        repository.get(7, definition.id)?.encodedValue shouldBe "not-an-int"
    }
}

private class FakeOverrideRepository : ViewerSettingOverrideRepository {
    private val overrides = MutableStateFlow<Map<Pair<Long, ViewerSettingId>, ViewerSettingOverride>>(emptyMap())

    override suspend fun get(entryId: Long, settingId: ViewerSettingId): ViewerSettingOverride? {
        return overrides.value[entryId to settingId]
    }

    override fun observe(entryId: Long, settingId: ViewerSettingId): Flow<ViewerSettingOverride?> {
        return overrides.map { it[entryId to settingId] }
    }

    override suspend fun getByEntryId(entryId: Long): List<ViewerSettingOverride> {
        return overrides.value.values.filter { it.entryId == entryId }
    }

    override suspend fun upsert(override: ViewerSettingOverride) {
        overrides.value = overrides.value + ((override.entryId to override.settingId) to override)
    }

    override suspend fun delete(entryId: Long, settingId: ViewerSettingId) {
        overrides.value = overrides.value - (entryId to settingId)
    }

    override suspend fun deleteByProviderForProfile(providerId: String, profileId: Long) = Unit
}

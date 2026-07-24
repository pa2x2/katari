package mihon.entry.interactions.settings

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import mihon.entry.viewer.settings.ViewerSettingCodecs
import mihon.entry.viewer.settings.ViewerSettingDefinition
import mihon.entry.viewer.settings.ViewerSettingId
import mihon.entry.viewer.settings.ViewerSettingOverride
import mihon.entry.viewer.settings.ViewerSettingOverrideRepository
import mihon.entry.viewer.settings.ViewerSettingScope
import mihon.entry.viewer.settings.ViewerSettingSource
import mihon.entry.viewer.settings.asProfilePreference
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
    fun `resolver applies default profile and entry precedence`() = runTest {
        val repository = FakeOverrideRepository()
        val binder = DefaultViewerSettingBinder(repository, backgroundScope, now = { 10 })

        binder.resolve(definition, entryId = 7).run {
            effectiveValue shouldBe 2
            source shouldBe ViewerSettingSource.PROCESSOR_DEFAULT
        }

        preference.set(3)
        binder.resolve(definition, entryId = 7).run {
            effectiveValue shouldBe 3
            source shouldBe ViewerSettingSource.PROFILE
        }

        repository.upsert(ViewerSettingOverride(7, definition.id, "1", 5))
        binder.resolve(definition, entryId = 7).run {
            effectiveValue shouldBe 1
            source shouldBe ViewerSettingSource.ENTRY
        }
    }

    @Test
    fun `binding writes and clears an override without copying profile value`() = runTest {
        preference.set(3)
        val repository = FakeOverrideRepository()
        val binding = DefaultViewerSettingBinder(repository, backgroundScope, now = { 10 })
            .bind(definition, entryId = 7)

        binding.setEntryOverride(1)
        binding.state.first { it.source == ViewerSettingSource.ENTRY }.effectiveValue shouldBe 1

        binding.clearEntryOverride()
        binding.state.first { it.source == ViewerSettingSource.PROFILE }.effectiveValue shouldBe 3
        repository.get(7, definition.id) shouldBe null
    }

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

    @Test
    fun `profile and entry resets affect only their own layers`() = runTest {
        val repository = FakeOverrideRepository()
        val binding = DefaultViewerSettingBinder(repository, backgroundScope).bind(definition, entryId = 7)
        binding.setProfileValue(3)
        binding.setEntryOverride(1)

        binding.resetProfileValue()
        DefaultViewerSettingBinder(repository, backgroundScope).resolve(definition, entryId = 7).run {
            effectiveValue shouldBe 1
            source shouldBe ViewerSettingSource.ENTRY
            profileValue shouldBe null
        }

        binding.clearEntryOverride()
        DefaultViewerSettingBinder(repository, backgroundScope).resolve(definition, entryId = 7).run {
            effectiveValue shouldBe 2
            source shouldBe ViewerSettingSource.PROCESSOR_DEFAULT
        }
    }

    @Test
    fun `profile preference adapter uses the shared binding`() = runTest {
        val binding = DefaultViewerSettingBinder(FakeOverrideRepository(), backgroundScope).bind(definition)
        val adapter = binding.asProfilePreference()

        adapter.get() shouldBe 2
        adapter.set(3)
        adapter.get() shouldBe 3
        adapter.delete()
        adapter.get() shouldBe 2
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

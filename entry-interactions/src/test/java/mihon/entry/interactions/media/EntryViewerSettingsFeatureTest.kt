package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mihon.entry.viewer.settings.ViewerSettingCodecs
import mihon.entry.viewer.settings.ViewerSettingDefinition
import mihon.entry.viewer.settings.ViewerSettingId
import mihon.entry.viewer.settings.ViewerSettingOverride
import mihon.entry.viewer.settings.ViewerSettingOverrideRepository
import mihon.entry.viewer.settings.ViewerSettingScope
import mihon.entry.viewer.settings.ViewerSettingsCategory
import mihon.entry.viewer.settings.ViewerSettingsProvider
import mihon.feature.graph.ContributionOwner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.core.common.preference.Preference
import tachiyomi.domain.entry.model.Entry

class EntryViewerSettingsFeatureTest {
    private val entry = Entry.create().copy(id = 11L, type = EntryType.BOOK)
    private val target = Entry.create().copy(id = 12L, type = EntryType.BOOK)

    @Test
    fun `provider absence is valid and owns no behaviors`() = runTest {
        val feature = featureFor()

        feature.isApplicable(EntryType.BOOK) shouldBe false
        feature.destinations shouldBe emptyList()
        feature.snapshot(entry) shouldBe EntryViewerSettingsSnapshotResult.Inapplicable(EntryType.BOOK)
    }

    @Test
    fun `provider surfaces reach projections backup and reset without implying Migration`() = runTest {
        val repository = mockk<ViewerSettingOverrideRepository>(relaxed = true)
        val first = surface("book.epub", ViewerSettingsCategory.READER)
        val second = surface("book.prose", ViewerSettingsCategory.READER)
        val stored = ViewerSettingOverride(entry.id, first.overrideSetting.id, "scroll", 1L)
        coEvery { repository.getByEntryId(entry.id) } returns listOf(stored)
        var legacyResetCount = 0
        val feature = featureFor(
            surfaces = listOf(first.provider, second.provider),
            projections = listOf(projection(first.provider.id), projection(second.provider.id)),
            repository = repository,
            legacyReset = {
                legacyResetCount++
                true
            },
        )

        feature.isApplicable(EntryType.BOOK) shouldBe true
        feature.destinations.map { it.surfaceId } shouldContainExactly listOf("book.epub", "book.prose")
        feature.snapshot(entry) shouldBe EntryViewerSettingsSnapshotResult.Available(listOf(stored))
        feature.restore(target, listOf(stored)) shouldBe EntryViewerSettingsRestoreResult.Restored(1, emptySet())
        feature.copy(entry, target) shouldBe EntryViewerSettingsCopyResult.Inapplicable(entry.type, target.type)
        feature.resetProfileOverrides(9L) shouldBe EntryViewerSettingsResetResult.Reset

        coVerify(exactly = 1) { repository.upsert(stored.copy(entryId = target.id)) }
        coVerify { repository.deleteByProviderForProfile("book.epub", 9L) }
        coVerify { repository.deleteByProviderForProfile("book.prose", 9L) }
        legacyResetCount shouldBe 1
    }

    @Test
    fun `missing and orphan projections fail with exact surface IDs`() {
        val surface = surface("book.epub", ViewerSettingsCategory.READER)

        assertThrows<IllegalStateException> {
            featureFor(surfaces = listOf(surface.provider))
        }.message shouldBe "Viewer Settings providers are missing app screen projections: [book.epub]"

        assertThrows<IllegalStateException> {
            featureFor(projections = listOf(projection("orphan.reader")))
        }.message shouldBe "Viewer Settings screen projections have no provider surface: [orphan.reader]"
    }

    @Test
    fun `restore rejects unknown and non override settings without manufacturing support`() = runTest {
        val repository = mockk<ViewerSettingOverrideRepository>(relaxed = true)
        val surface = surface("book.epub", ViewerSettingsCategory.READER)
        val feature = featureFor(
            surfaces = listOf(surface.provider),
            projections = listOf(projection(surface.provider.id)),
            repository = repository,
        )
        val unknown = ViewerSettingOverride(entry.id, ViewerSettingId("unknown", "layout"), "scroll", 1L)
        val profileOnly = ViewerSettingOverride(entry.id, surface.profileSetting.id, "state", 1L)

        val result = feature.restore(entry, listOf(unknown, profileOnly))
            .shouldBeInstanceOf<EntryViewerSettingsRestoreResult.Restored>()

        result.restoredCount shouldBe 0
        result.rejectedSettingIds shouldBe setOf(unknown.settingId, profileOnly.settingId)
        coVerify(exactly = 0) { repository.upsert(any()) }
    }

    @Test
    fun `migration payload includes provider-owned legacy normalization and portable overrides`() = runTest {
        val repository = mockk<ViewerSettingOverrideRepository>(relaxed = true)
        val surface = surface("book.epub", ViewerSettingsCategory.READER)
        val stored = ViewerSettingOverride(entry.id, surface.overrideSetting.id, "scroll", 4L)
        coEvery { repository.getByEntryId(entry.id) } returns listOf(stored)
        var storedFlags: Triple<Long, Long, Long>? = null
        val feature = featureFor(
            surfaces = listOf(surface.provider),
            projections = listOf(projection(surface.provider.id)),
            repository = repository,
            normalization = { flags -> flags and 0x3FL.inv() },
            migrationStore = { entryId, profileId, flags ->
                storedFlags = Triple(entryId, profileId, flags)
                true
            },
            migration = true,
        )
        val source = entry.copy(viewerFlags = 0x7FL)

        val prepared = feature.prepareMigration(source, target)
            as EntryViewerSettingsMigrationPreparation.Prepared

        prepared.payload.normalizedViewerFlags shouldBe 0x40L
        prepared.payload.overrides shouldBe listOf(
            EntryViewerSettingMigrationValue("book.epub", "layout", "scroll", 4L),
        )
        feature.applyMigration(prepared.payload) shouldBe EntryViewerSettingsRestoreResult.Restored(1, emptySet())
        storedFlags shouldBe Triple(target.id, target.profileId, 0x40L)
        coVerify { repository.upsert(stored.copy(entryId = target.id)) }
    }

    private fun featureFor(
        surfaces: List<ViewerSettingsProvider> = emptyList(),
        projections: List<EntryViewerSettingsScreenProjection> = emptyList(),
        repository: ViewerSettingOverrideRepository = mockk(relaxed = true),
        legacyReset: suspend () -> Boolean = { true },
        normalization: (Long) -> Long = { it },
        migrationStore: suspend (Long, Long, Long) -> Boolean = { _, _, _ -> true },
        migration: Boolean = false,
    ): EntryViewerSettingsFeature {
        val bindings = buildList {
            if (surfaces.isNotEmpty()) {
                add(
                    EntryViewerSettingsCapability.bind(
                        DefaultEntryViewerSettingsProvider(EntryType.BOOK, surfaces, normalization),
                    ),
                )
            }
            if (migration) add(EntryMigrationCapability.bind(MigrationProvider()))
        }
        val composition = createEntryInteractionComposition(
            plugins = listOf(
                object : EntryInteractionPlugin {
                    override val type = EntryType.BOOK
                    override val owner = ContributionOwner("test.viewer-settings.book")
                    override val providerBindings = bindings
                },
            ),
            featureContributors = listOf(EntryViewerSettingsFeatureContributor),
        )
        return DefaultEntryViewerSettingsFeature(
            evaluation = composition.featureGraphEvaluation,
            interaction = composition.interactions.viewerSettings,
            projectionResolver = EntryViewerSettingsScreenProjectionResolver { projections },
            overrideRepository = repository,
            legacyMangaViewerFlagsReset = EntryLegacyMangaViewerFlagsReset { legacyReset() },
            migrationStore = EntryViewerFlagsMigrationStore(migrationStore),
        )
    }

    private fun surface(id: String, category: ViewerSettingsCategory): TestSurface {
        val store = InMemoryPreferenceStore()
        val overrideSetting = ViewerSettingDefinition(
            id = ViewerSettingId(id, "layout"),
            scope = ViewerSettingScope.PROFILE_WITH_ENTRY_OVERRIDE,
            processorDefault = "page",
            profilePreference = store.getString("$id.layout", "page"),
            codec = ViewerSettingCodecs.String,
        )
        val profileSetting = ViewerSettingDefinition(
            id = ViewerSettingId(id, "session"),
            scope = ViewerSettingScope.PROFILE_ONLY,
            processorDefault = "state",
            profilePreference = store.getString(Preference.appStateKey("$id.session"), "state"),
            codec = ViewerSettingCodecs.String,
        )
        val privateSetting = ViewerSettingDefinition(
            id = ViewerSettingId(id, "secret"),
            scope = ViewerSettingScope.PROFILE_ONLY,
            processorDefault = "secret",
            profilePreference = store.getString(Preference.privateKey("$id.secret"), "secret"),
            codec = ViewerSettingCodecs.String,
        )
        return TestSurface(
            provider = object : ViewerSettingsProvider {
                override val id = id
                override val category = category
                override val displayName = id
                override val settings = listOf(overrideSetting, profileSetting, privateSetting)
            },
            overrideSetting = overrideSetting,
            profileSetting = profileSetting,
        )
    }

    private fun projection(id: String) = object : EntryViewerSettingsScreenProjection {
        override val surfaceId = id
    }

    private data class TestSurface(
        val provider: ViewerSettingsProvider,
        val overrideSetting: ViewerSettingDefinition<String>,
        val profileSetting: ViewerSettingDefinition<String>,
    )

    private class MigrationProvider : EntryMigrationProvider {
        override val type = EntryType.BOOK
    }
}

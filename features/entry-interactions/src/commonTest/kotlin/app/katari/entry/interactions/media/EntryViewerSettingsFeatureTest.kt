package app.katari.entry.interactions.media

import app.katari.entry.interactions.runtime.DefaultEntryInteractions
import app.katari.entry.viewer.settings.ViewerSettingCodecs
import app.katari.entry.viewer.settings.ViewerSettingDefinition
import app.katari.entry.viewer.settings.ViewerSettingId
import app.katari.entry.viewer.settings.ViewerSettingOverride
import app.katari.entry.viewer.settings.ViewerSettingOverrideRepository
import app.katari.entry.viewer.settings.ViewerSettingScope
import app.katari.entry.viewer.settings.ViewerSettingsCategory
import app.katari.entry.viewer.settings.ViewerSettingsProvider
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.core.common.preference.Preference
import tachiyomi.domain.entry.model.Entry

class EntryViewerSettingsFeatureTest {
    private val entry = Entry.create().copy(id = 11L, type = EntryType.BOOK)

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

    private fun featureFor(
        surfaces: List<ViewerSettingsProvider>,
        projections: List<EntryViewerSettingsScreenProjection>,
        repository: ViewerSettingOverrideRepository,
    ): EntryViewerSettingsFeature {
        val interactions = DefaultEntryInteractions(
            viewerSettingsProviders = mapOf(EntryType.BOOK to DefaultEntryViewerSettingsProvider(surfaces)),
        )
        return DefaultEntryViewerSettingsFeature(
            applicableTypes = setOf(EntryType.BOOK),
            viewerTypes = setOf(EntryType.BOOK),
            migrationTypes = emptySet(),
            interaction = interactions.viewerSettings,
            projectionResolver = EntryViewerSettingsScreenProjectionResolver { projections },
            overrideRepository = repository,
            legacyMangaViewerFlagsReset = EntryLegacyMangaViewerFlagsReset { true },
            migrationStore = EntryViewerFlagsMigrationStore { _, _, _ -> true },
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
}

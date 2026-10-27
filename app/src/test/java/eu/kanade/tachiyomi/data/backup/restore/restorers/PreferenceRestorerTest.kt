package eu.kanade.tachiyomi.data.backup.restore.restorers

import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.data.backup.models.BackupPreference
import eu.kanade.tachiyomi.data.backup.models.BooleanPreferenceValue
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mihon.domain.extension.model.ContentWarning
import mihon.feature.profiles.core.ProfileStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.FakePreferenceStore

class PreferenceRestorerTest {

    // A backup made before content warnings hid NSFW extensions, and the restored profile would load them
    @Test
    fun `a backup's hidden NSFW extensions stay hidden in the restored profile`() = runTest {
        val profile = FakePreferenceStore()
        val restorer = PreferenceRestorer(
            context = mockk(),
            getCategories = mockk(),
            preferenceStore = FakePreferenceStore(),
            profileStore = mockk<ProfileStore> { every { profileStore(2L) } returns profile },
            sourcePreferenceFiles = mockk(),
            categoryRepository = mockk(),
            updateRulesRepository = mockk(),
        )

        restorer.restoreAppForProfile(
            profileId = 2L,
            preferences = listOf(
                BackupPreference(SourcePreferences.LEGACY_SHOW_NSFW_SOURCE_KEY, BooleanPreferenceValue(false)),
            ),
            backupCategories = null,
            includeGlobalRestore = false,
            scheduleJobs = false,
        )

        assertEquals(setOf(ContentWarning.SAFE), SourcePreferences.enabledContentWarnings(profile).get())
        assertFalse(profile.getBoolean(SourcePreferences.LEGACY_SHOW_NSFW_SOURCE_KEY, true).isSet())
    }
}

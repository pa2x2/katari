package mihon.feature.profiles.core

import android.app.Application
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.adapter.LegacyMangaSourceAdapter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.SMangaUpdate
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.EntryDestructiveRemovalFeature
import mihon.entry.interactions.EntryDestructiveRemovalResult
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.ProfilePreferenceOwnerRegistry
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryRepository

class ProfileManagerTest {

    @Test
    fun `create profile seeds hidden sources from installed extensions`() = runTest {
        val insertedProfileId = 2L
        val profileDatabase = mockk<ProfileDatabase>()
        val profileStores = mutableMapOf<Long, TestPreferenceStore>()
        val appStateStores = mutableMapOf<Long, TestPreferenceStore>()
        val privateStores = mutableMapOf<Long, TestPreferenceStore>()
        val profileStore = mockk<ProfileStoreImpl>()
        val profilesPreferences = ProfilesPreferences(TestPreferenceStore())
        val extensionManager = mockk<ExtensionManager>()
        val entryRepository = mockk<EntryRepository>(relaxed = true)
        val destructiveRemoval = mockk<EntryDestructiveRemovalFeature>(relaxed = true)
        val existingProfiles = listOf(defaultProfile())

        coEvery { profileDatabase.subscribeProfiles(any()) } returns flowOf(existingProfiles)
        coEvery { profileDatabase.getProfiles(includeArchived = true) } returns existingProfiles
        coEvery {
            profileDatabase.insertProfile(
                uuid = any(),
                name = "Anime",
                colorSeed = any(),
                position = 1L,
                requiresAuth = false,
                isArchived = false,
            )
        } returns insertedProfileId
        coEvery { profileDatabase.getProfileById(insertedProfileId) } returns Profile(
            id = insertedProfileId,
            uuid = "profile-$insertedProfileId",
            name = "Anime",
            colorSeed = 1L,
            position = 1L,
            requiresAuth = false,
            isArchived = false,
        )
        every { extensionManager.installedExtensionsFlow } returns MutableStateFlow(
            listOf(
                installedExtension(
                    "manga",
                    listOf(FakeMangaSource(10L, "Manga A"), FakeMangaSource(11L, "Manga B")),
                ),
            ),
        )
        every { profileStore.currentProfileId } returns ProfileConstants.DEFAULT_PROFILE_ID
        every { profileStore.currentProfileIdFlow } returns flowOf(ProfileConstants.DEFAULT_PROFILE_ID)
        every { profileStore.activeProfileId } returns ProfileConstants.DEFAULT_PROFILE_ID
        every { profileStore.activeProfileIdFlow } returns flowOf(ProfileConstants.DEFAULT_PROFILE_ID)
        every { profileStore.profileStore(any()) } answers {
            profileStores.getOrPut(firstArg()) { TestPreferenceStore() }
        }
        every { profileStore.profileStore() } answers {
            profileStores.getOrPut(ProfileConstants.DEFAULT_PROFILE_ID) { TestPreferenceStore() }
        }
        every { profileStore.appStateStore(any()) } answers {
            appStateStores.getOrPut(firstArg()) { TestPreferenceStore() }
        }
        every { profileStore.appStateStore() } answers {
            appStateStores.getOrPut(ProfileConstants.DEFAULT_PROFILE_ID) { TestPreferenceStore() }
        }
        every { profileStore.privateStore(any()) } answers {
            privateStores.getOrPut(firstArg()) { TestPreferenceStore() }
        }
        every { profileStore.privateStore() } answers {
            privateStores.getOrPut(ProfileConstants.DEFAULT_PROFILE_ID) { TestPreferenceStore() }
        }
        every { profileStore.basePreferenceStore() } returns TestPreferenceStore()
        every { profileStore.deleteProfileState(insertedProfileId) } answers {
            profileStores.remove(insertedProfileId)
            appStateStores.remove(insertedProfileId)
            privateStores.remove(insertedProfileId)
        }

        val manager = ProfileManager(
            application = mockk<Application>(relaxed = true),
            profileDatabase = profileDatabase,
            profileStore = profileStore,
            profilesPreferences = profilesPreferences,
            extensionManager = extensionManager,
            preferenceOwnership = ProfilePreferenceOwnership(ProfilePreferenceOwnerRegistry()),
            entryRepository = entryRepository,
            destructiveRemoval = destructiveRemoval,
        )

        val profile = manager.createProfile(name = "Anime")

        profile.id shouldBe insertedProfileId
        profileStore.profileStore(insertedProfileId)
            .getStringSet(SourcePreferences.HIDDEN_SOURCES_KEY, emptySet())
            .get() shouldContainExactlyInAnyOrder setOf("10", "11")
    }

    @Test
    fun `permanent deletion removes profile entries through the destructive removal Feature`() = runTest {
        val profileId = 2L
        val profile = Profile(
            id = profileId,
            uuid = "profile-$profileId",
            name = "Archived",
            colorSeed = 1L,
            position = 1L,
            requiresAuth = false,
            isArchived = true,
        )
        val entries = listOf(Entry.create().copy(id = 7L, profileId = profileId))
        val profileDatabase = mockk<ProfileDatabase> {
            coEvery { subscribeProfiles(any()) } returns flowOf(listOf(defaultProfile(), profile))
            coEvery { getProfileById(profileId) } returns profile
            coEvery { deleteProfile(profileId) } just runs
        }
        val profileStore = mockk<ProfileStoreImpl> {
            every { currentProfileId } returns ProfileConstants.DEFAULT_PROFILE_ID
            every { deleteProfileState(profileId) } just runs
        }
        val entryRepository = mockk<EntryRepository> {
            coEvery { getAllEntriesByProfile(profileId) } returns entries
        }
        val destructiveRemoval = mockk<EntryDestructiveRemovalFeature> {
            coEvery { remove(entries) } returns EntryDestructiveRemovalResult.Removed(entries, emptyList())
        }
        val manager = ProfileManager(
            application = mockk(relaxed = true),
            profileDatabase = profileDatabase,
            profileStore = profileStore,
            profilesPreferences = ProfilesPreferences(TestPreferenceStore()),
            extensionManager = mockk(relaxed = true),
            preferenceOwnership = ProfilePreferenceOwnership(ProfilePreferenceOwnerRegistry()),
            entryRepository = entryRepository,
            destructiveRemoval = destructiveRemoval,
        )

        manager.permanentlyDeleteProfile(profileId)

        coVerifyOrder {
            entryRepository.getAllEntriesByProfile(profileId)
            destructiveRemoval.remove(entries)
            profileDatabase.deleteProfile(profileId)
        }
        verify(exactly = 1) { profileStore.deleteProfileState(profileId) }
    }

    @Test
    fun `permanent deletion preserves profile when destructive removal fails transactionally`() = runTest {
        val profileId = 2L
        val profile = Profile(
            id = profileId,
            uuid = "profile-$profileId",
            name = "Archived",
            colorSeed = 1L,
            position = 1L,
            requiresAuth = false,
            isArchived = true,
        )
        val entries = listOf(Entry.create().copy(id = 7L, profileId = profileId))
        val failure = IllegalStateException("removal failed")
        val profileDatabase = mockk<ProfileDatabase> {
            coEvery { subscribeProfiles(any()) } returns flowOf(listOf(defaultProfile(), profile))
            coEvery { getProfileById(profileId) } returns profile
        }
        val profileStore = mockk<ProfileStoreImpl> {
            every { currentProfileId } returns ProfileConstants.DEFAULT_PROFILE_ID
        }
        val entryRepository = mockk<EntryRepository> {
            coEvery { getAllEntriesByProfile(profileId) } returns entries
        }
        val destructiveRemoval = mockk<EntryDestructiveRemovalFeature> {
            coEvery { remove(entries) } returns EntryDestructiveRemovalResult.Failed(entries, failure)
        }
        val manager = ProfileManager(
            application = mockk(relaxed = true),
            profileDatabase = profileDatabase,
            profileStore = profileStore,
            profilesPreferences = ProfilesPreferences(TestPreferenceStore()),
            extensionManager = mockk(relaxed = true),
            preferenceOwnership = ProfilePreferenceOwnership(ProfilePreferenceOwnerRegistry()),
            entryRepository = entryRepository,
            destructiveRemoval = destructiveRemoval,
        )

        assertThrows<IllegalStateException> {
            manager.permanentlyDeleteProfile(profileId)
        }

        coVerifyOrder {
            entryRepository.getAllEntriesByProfile(profileId)
            destructiveRemoval.remove(entries)
        }
        coVerify(exactly = 0) { profileDatabase.deleteProfile(profileId) }
        verify(exactly = 0) { profileStore.deleteProfileState(profileId) }
    }

    private fun defaultProfile() = Profile(
        id = ProfileConstants.DEFAULT_PROFILE_ID,
        uuid = ProfileConstants.DEFAULT_PROFILE_UUID,
        name = ProfileConstants.DEFAULT_PROFILE_NAME,
        colorSeed = 0L,
        position = 0L,
        requiresAuth = false,
        isArchived = false,
    )

    private fun installedExtension(name: String, sources: List<Source>) = Extension.Installed(
        name = name,
        pkgName = "$name.pkg",
        versionName = "1.0.0",
        versionCode = 1L,
        libVersion = 1.5,
        lang = "en",
        isNsfw = false,
        pkgFactory = null,
        sources = sources.map { LegacyMangaSourceAdapter(it) },
        icon = null,
        isShared = false,
    )
}

private data class FakeMangaSource(
    override val id: Long,
    override val name: String,
    override val lang: String = "en",
) : Source {
    override val supportsLatest: Boolean = false

    override suspend fun getPopularManga(page: Int): MangasPage = error("Not used")

    override suspend fun getLatestUpdates(page: Int): MangasPage = error("Not used")

    override suspend fun getSearchManga(page: Int, query: String, filters: FilterList): MangasPage = error("Not used")

    override suspend fun getMangaUpdate(
        manga: SManga,
        chapters: List<SChapter>,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ): SMangaUpdate = SMangaUpdate(manga, chapters)

    override suspend fun getPageList(chapter: SChapter): List<Page> = error("Not used")
}

private class TestPreferenceStore : PreferenceStore {
    private val backing = mutableMapOf<String, Any?>()

    override fun getString(key: String, defaultValue: String): Preference<String> = TestPreference(key, defaultValue)

    override fun getLong(key: String, defaultValue: Long): Preference<Long> = TestPreference(key, defaultValue)

    override fun getInt(key: String, defaultValue: Int): Preference<Int> = TestPreference(key, defaultValue)

    override fun getFloat(key: String, defaultValue: Float): Preference<Float> = TestPreference(key, defaultValue)

    override fun getBoolean(key: String, defaultValue: Boolean): Preference<Boolean> = TestPreference(key, defaultValue)

    override fun getStringSet(key: String, defaultValue: Set<String>): Preference<Set<String>> = TestPreference(
        key,
        defaultValue,
    )

    override fun <T> getObjectFromString(
        key: String,
        defaultValue: T,
        serializer: (T) -> String,
        deserializer: (String) -> T,
    ): Preference<T> = TestPreference(key, defaultValue)

    override fun <T> getObjectFromInt(
        key: String,
        defaultValue: T,
        serializer: (T) -> Int,
        deserializer: (Int) -> T,
    ): Preference<T> = TestPreference(key, defaultValue)

    override fun <T> getObjectSetFromStringSet(
        key: String,
        defaultValue: Set<T>,
        serializer: (T) -> String,
        deserializer: (String) -> T?,
    ): Preference<Set<T>> = TestPreference(key, defaultValue)

    override fun getAll(): Map<String, *> = backing.toMap()

    private inner class TestPreference<T>(
        private val key: String,
        private val defaultValue: T,
    ) : Preference<T> {
        private val state = MutableStateFlow(get())

        override fun key(): String = key

        @Suppress("UNCHECKED_CAST")
        override fun get(): T = backing[key] as T? ?: defaultValue

        override fun set(value: T) {
            backing[key] = value
            state.value = value
        }

        override fun isSet(): Boolean = key in backing

        override fun delete() {
            backing.remove(key)
            state.value = defaultValue
        }

        override fun defaultValue(): T = defaultValue

        override fun changes(): Flow<T> = state

        override fun stateIn(scope: CoroutineScope): StateFlow<T> = state
    }
}

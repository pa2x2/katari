package eu.kanade.presentation.more.settings.screen.translation.series

import cafe.adriel.voyager.core.annotation.InternalVoyagerApi
import cafe.adriel.voyager.core.model.ScreenModelStore
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mihon.language.api.tag.LanguageTag
import org.junit.jupiter.api.Test
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryTranslationLanguages
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.repository.EntryTranslationLanguagesRepository

@OptIn(ExperimentalCoroutinesApi::class, InternalVoyagerApi::class)
class SeriesTranslationLanguagesScreenModelTest {
    @Test
    fun `lists the profile's series with their languages and clears one or all of them`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val repository = ProfileLanguagesRepository(
            PROFILE_ID to EntryTranslationLanguages(ONE_PIECE.id, "ja", "es", 0L),
            PROFILE_ID to EntryTranslationLanguages(SOLO_LEVELING.id, "ko", null, 0L),
            PROFILE_ID to EntryTranslationLanguages(YOTSUBA.id, null, "fr", 0L),
            OTHER_PROFILE_ID to EntryTranslationLanguages(OTHER.id, null, "de", 0L),
        )
        val entryRepository = mockk<EntryRepository> {
            coEvery { getEntriesByIds(any()) } answers {
                firstArg<List<Long>>().mapNotNull { id -> ENTRIES.firstOrNull { it.id == id } }
            }
        }
        val model = ScreenModelStore.getOrPut(STORE_KEY, null) {
            SeriesTranslationLanguagesScreenModel(repository, entryRepository, ActiveProfile(PROFILE_ID))
        }
        val collector = backgroundScope.launch { model.series.collect {} }

        try {
            advanceUntilIdle()
            model.series.value?.map { Triple(it.title, it.contentLanguage, it.targetLanguage) } shouldBe listOf(
                Triple("One Piece", LanguageTag.require("ja"), LanguageTag.require("es")),
                Triple("Solo Leveling", LanguageTag.require("ko"), null),
                Triple("Yotsuba&!", null, LanguageTag.require("fr")),
            )

            model.clear(SOLO_LEVELING.id)
            advanceUntilIdle()
            model.series.value?.map { it.title } shouldBe listOf("One Piece", "Yotsuba&!")

            model.clearAll()
            advanceUntilIdle()
            model.series.value shouldBe emptyList()
            repository.recordsOf(OTHER_PROFILE_ID).map { it.entryId } shouldBe listOf(OTHER.id)
        } finally {
            collector.cancel()
            ScreenModelStore.onDisposeNavigator(STORE_KEY)
            advanceUntilIdle()
            Dispatchers.resetMain()
        }
    }

    /** Lists and deletes by profile in insertion order, standing in for the database's title order. */
    private class ProfileLanguagesRepository(
        vararg records: Pair<Long, EntryTranslationLanguages>,
    ) : EntryTranslationLanguagesRepository {
        private val records = MutableStateFlow(records.toList())

        fun recordsOf(profileId: Long) = records.value.filter { it.first == profileId }.map { it.second }

        override fun subscribeByProfile(profileId: Long): Flow<List<EntryTranslationLanguages>> =
            records.map { all -> all.filter { it.first == profileId }.map { it.second } }

        override suspend fun delete(entryId: Long) = records.update { all ->
            all.filterNot {
                it.second.entryId ==
                    entryId
            }
        }

        override suspend fun deleteByProfile(profileId: Long) = records.update { all ->
            all.filterNot {
                it.first ==
                    profileId
            }
        }

        override suspend fun getByEntryId(entryId: Long) = records.value.firstOrNull {
            it.second.entryId == entryId
        }?.second

        override fun subscribeByEntryId(entryId: Long) = flowOf(null)

        override suspend fun upsert(languages: EntryTranslationLanguages) = error("Not used by the list")

        override suspend fun setContentLanguage(entryId: Long, language: String?, updatedAt: Long) =
            error("Not used by the list")

        override suspend fun setTargetLanguage(entryId: Long, language: String?, updatedAt: Long) =
            error("Not used by the list")
    }

    private class ActiveProfile(override val activeProfileId: Long) : ActiveProfileProvider {
        override val activeProfileIdFlow: Flow<Long> = flowOf(activeProfileId)
    }

    private companion object {
        const val STORE_KEY = "series-translation-languages-test"
        const val PROFILE_ID = 1L
        const val OTHER_PROFILE_ID = 2L
        val ONE_PIECE = Entry.create().copy(id = 10L, title = "One Piece")
        val SOLO_LEVELING = Entry.create().copy(id = 11L, title = "Solo Leveling")
        val YOTSUBA = Entry.create().copy(id = 12L, title = "Yotsuba&!")
        val OTHER = Entry.create().copy(id = 20L, title = "Other profile's series")
        val ENTRIES = listOf(ONE_PIECE, SOLO_LEVELING, YOTSUBA, OTHER)
    }
}

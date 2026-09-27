package tachiyomi.data.entry

import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.data.AndroidDatabaseHandler
import tachiyomi.data.Chapters
import tachiyomi.data.Database
import tachiyomi.data.DateColumnAdapter
import tachiyomi.data.Entries
import tachiyomi.data.History
import tachiyomi.data.MemoColumnAdapter
import tachiyomi.data.StringListColumnAdapter
import tachiyomi.data.UpdateStrategyColumnAdapter
import tachiyomi.domain.entry.model.EntryTranslationLanguages

class EntryTranslationLanguagesRepositoryImplTest {

    @Test
    fun `clearing one language keeps the other and clearing both removes the record`() = withRepository {
        setTargetLanguage(entryId = 10, language = "es", updatedAt = 1)
        setContentLanguage(entryId = 10, language = "ja", updatedAt = 2)
        getByEntryId(10) shouldBe EntryTranslationLanguages(10, "ja", "es", updatedAt = 2)

        setTargetLanguage(entryId = 10, language = null, updatedAt = 3)
        getByEntryId(10) shouldBe EntryTranslationLanguages(10, "ja", null, updatedAt = 3)

        setContentLanguage(entryId = 10, language = null, updatedAt = 4)
        getByEntryId(10) shouldBe null

        setContentLanguage(entryId = 10, language = null, updatedAt = 5)
        getByEntryId(10) shouldBe null
    }

    @Test
    fun `profile records follow entry titles and are cleared per profile`() = withRepository {
        setTargetLanguage(entryId = 10, language = "es", updatedAt = 1)
        setContentLanguage(entryId = 11, language = "fr", updatedAt = 1)
        setTargetLanguage(entryId = 20, language = "de", updatedAt = 1)

        subscribeByProfile(2).first().map { it.entryId } shouldBe listOf(11L, 10L)

        deleteByProfile(2)

        subscribeByProfile(2).first() shouldBe emptyList()
        getByEntryId(20) shouldBe EntryTranslationLanguages(20, null, "de", updatedAt = 1)
    }

    private fun withRepository(block: suspend EntryTranslationLanguagesRepositoryImpl.() -> Unit) = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            Database.Schema.awaitCreate(driver)
            driver.await(
                identifier = null,
                sql = """
                    INSERT INTO profiles(_id, uuid, name, color_seed, position)
                    VALUES (2, 'second', 'Second', 1, 1), (3, 'third', 'Third', 2, 2)
                """.trimIndent(),
                parameters = 0,
            )
            driver.await(
                identifier = null,
                sql = """
                    INSERT INTO entries(_id, profile_id, source, url, title)
                    VALUES (10, 2, 1, '/yotsuba', 'Yotsuba'), (11, 2, 1, '/miserables', 'Les Misérables'),
                        (20, 3, 1, '/other', 'Other profile')
                """.trimIndent(),
                parameters = 0,
            )
            EntryTranslationLanguagesRepositoryImpl(AndroidDatabaseHandler(database(driver), driver)).block()
        } finally {
            driver.close()
        }
    }

    private fun database(driver: JdbcSqliteDriver): Database {
        return Database(
            driver = driver,
            entriesAdapter = Entries.Adapter(
                genreAdapter = StringListColumnAdapter,
                update_strategyAdapter = UpdateStrategyColumnAdapter,
                memoAdapter = MemoColumnAdapter,
            ),
            chaptersAdapter = Chapters.Adapter(memoAdapter = MemoColumnAdapter),
            historyAdapter = History.Adapter(last_readAdapter = DateColumnAdapter),
        )
    }
}

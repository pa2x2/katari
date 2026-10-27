package tachiyomi.data.source.identity

import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.async.coroutines.awaitMigrate
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
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
import tachiyomi.domain.source.identity.SourceIdScheme
import tachiyomi.domain.source.identity.SourceIdentity

class SourceIdentityRepositoryImplTest {

    @Test
    fun `migration keeps every stored source key and later keys are allocated below them`() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            listOf(
                "CREATE TABLE sources(_id INTEGER NOT NULL PRIMARY KEY, lang TEXT NOT NULL, name TEXT NOT NULL)",
                "CREATE TABLE entries(_id INTEGER PRIMARY KEY, source INTEGER NOT NULL)",
                "CREATE TABLE source_migration_sessions(session_id TEXT PRIMARY KEY, origin_source_id INTEGER NOT NULL)",
                "CREATE TABLE source_migration_target_sources(session_id TEXT, source_id INTEGER NOT NULL)",
                "CREATE TABLE source_migration_group_members(session_id TEXT, source_id INTEGER NOT NULL)",
                "CREATE TABLE source_migration_items(session_id TEXT, source_id INTEGER NOT NULL, target_source_id INTEGER)",
                "CREATE TABLE source_migration_candidates(session_id TEXT, target_source_id INTEGER NOT NULL)",
                "CREATE TABLE source_migration_discovery_failures(session_id TEXT, target_source_id INTEGER NOT NULL)",
                "INSERT INTO sources VALUES (2499283573021220255, 'en', 'MangaDex'), (-7, '', 'Hard-coded')",
                "INSERT INTO entries VALUES (1, 0), (2, 2499283573021220255), (3, 1998944621602463790)",
                "INSERT INTO source_migration_sessions VALUES ('s', 1998944621602463790)",
                "INSERT INTO source_migration_items VALUES ('s', 1998944621602463790, NULL)",
                "INSERT INTO source_migration_candidates VALUES ('s', 500000001)",
            ).forEach { driver.await(null, it, 0) }

            Database.Schema.awaitMigrate(driver, oldVersion = 54, newVersion = 55)

            val repository = driver.repository()
            repository.findAll().map { it.localKey to it.identity }.shouldContainExactlyInAnyOrder(
                0L to SourceIdentity.LOCAL,
                -7L to tachiyomi(-7),
                500000001L to tachiyomi(500000001),
                1998944621602463790L to tachiyomi(1998944621602463790),
                2499283573021220255L to tachiyomi(2499283573021220255),
            )
            repository.register(SourceIdentity(SourceIdScheme.ANIYOMI, 2499283573021220255)) shouldBe -8L
        } finally {
            driver.close()
        }
    }

    @Test
    fun `an identity whose ID is another identity's key gets a key of its own that it keeps`() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            Database.Schema.awaitCreate(driver)
            val repository = driver.repository()
            val anime = SourceIdentity(SourceIdScheme.ANIYOMI, 5)

            repository.register(tachiyomi(5)) shouldBe 5L
            repository.register(anime) shouldBe -2L
            repository.register(SourceIdentity(SourceIdScheme.ANIYOMI, 9)) shouldBe -3L
            repository.register(tachiyomi(-3)) shouldBe -4L
            repository.register(tachiyomi(0)) shouldBe -5L
            repository.register(anime) shouldBe -2L
            repository.register(tachiyomi(5)) shouldBe 5L
            repository.register(SourceIdentity.LOCAL) shouldBe 0L
        } finally {
            driver.close()
        }
    }

    private fun tachiyomi(id: Long) = SourceIdentity(SourceIdScheme.TACHIYOMI, id)

    private fun JdbcSqliteDriver.repository(): SourceIdentityRepositoryImpl {
        val database = Database(
            driver = this,
            entriesAdapter = Entries.Adapter(
                genreAdapter = StringListColumnAdapter,
                update_strategyAdapter = UpdateStrategyColumnAdapter,
                memoAdapter = MemoColumnAdapter,
            ),
            chaptersAdapter = Chapters.Adapter(memoAdapter = MemoColumnAdapter),
            historyAdapter = History.Adapter(last_readAdapter = DateColumnAdapter),
        )
        return SourceIdentityRepositoryImpl(AndroidDatabaseHandler(db = database, driver = this))
    }
}

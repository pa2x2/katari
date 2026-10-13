package eu.kanade.tachiyomi.ui.base.migration

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import mihon.core.migration.Migrator
import mihon.domain.database.repository.DatabaseRepository
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * Whether an activity should show the app migrating screen instead of its content: true while the app migrations or
 * a database schema migration after an update run. The content must not be composed until then, since it reads state
 * the migrations may still be rewriting.
 */
@Composable
fun rememberAppMigrating(): Boolean {
    val databaseRepository = remember { Injekt.get<DatabaseRepository>() }
    var appMigrating by remember { mutableStateOf(Migrator.isRunning) }
    LaunchedEffect(Unit) {
        Migrator.awaitAndRelease()
        appMigrating = false
    }
    // Uncaught on purpose: a failed database migration otherwise leaves the migrating screen up for good, while the
    // crash screen tells what went wrong and lets the logs be shared
    LaunchedEffect(Unit) { databaseRepository.awaitOpen() }
    val databaseMigrating by databaseRepository.isMigrating.collectAsState()

    val migrating = appMigrating || databaseMigrating
    var migratingSince by remember { mutableStateOf(if (migrating) TimeSource.Monotonic.markNow() else null) }
    LaunchedEffect(migrating) {
        val since = migratingSince
        if (migrating) {
            if (since == null) migratingSince = TimeSource.Monotonic.markNow()
        } else if (since != null) {
            // A migration that finishes right away would otherwise only flash the screen
            delay(MIGRATING_SCREEN_MIN_DURATION - since.elapsedNow())
            migratingSince = null
        }
    }
    return migrating || migratingSince != null
}

private val MIGRATING_SCREEN_MIN_DURATION = 1.seconds

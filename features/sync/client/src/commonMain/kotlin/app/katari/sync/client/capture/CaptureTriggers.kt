package app.katari.sync.client.capture

import app.katari.data.database.TemporarySchemaPart
import app.katari.sync.client.kind.SyncKinds
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides

/**
 * Every kind's capture triggers, on every connection from the database's first write after its schema migrations
 * ([app.katari.data.database.TemporarySchema]). They record nothing while `sync_state.enabled` is off, so turning sync
 * on or off takes effect with the transaction that sets it, and no write made at process start, by a background job
 * or a notification's action, goes uncaptured while something else gets ready.
 */
@ContributesTo(AppScope::class)
@BindingContainer
object CaptureTriggers {
    internal val statements: List<String> = SyncKinds.all.flatMap { kind ->
        kind.capture.mapIndexed { index, trigger -> trigger.createStatement(kind.name, index) }
    }

    @Provides
    @IntoSet
    fun schemaPart(): TemporarySchemaPart = TemporarySchemaPart(statements)
}

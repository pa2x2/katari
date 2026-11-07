package app.katari.sync.client.run

/**
 * A step of the run under way that can take a while, [done] of [total] so far. A first download or full resync is not
 * one: its stage and position say how far it got ([app.katari.sync.client.connection.SyncConnectionState.stageTarget]).
 */
data class SyncStep(val kind: Kind, val done: Long, val total: Long) {
    /** The step as the status shows it: whole percents of a step shown in percent, the items of one shown as a count. */
    fun shown(): SyncStep = if (kind.shownAsCount) {
        this
    } else {
        SyncStep(
            kind,
            if (total >
                0
            ) {
                done * 100 / total
            } else {
                0
            },
            100,
        )
    }

    enum class Kind(val shownAsCount: Boolean) {
        /** This device's library recorded for the account on signing in, counted in rows. */
        PREPARING(shownAsCount = false),

        /** Changes made here going to the server, counted in records, such as a whole library after signing in. */
        UPLOADING_RECORDS(shownAsCount = false),

        /** Custom covers and fonts the server lacks, counted in files. */
        UPLOADING_FILES(shownAsCount = true),

        /** Custom covers and fonts the records name that aren't here yet, counted in files. */
        DOWNLOADING_FILES(shownAsCount = true),

        /** Other devices' changes since this device's position, counted in server sequences. */
        DOWNLOADING_CHANGES(shownAsCount = false),
    }
}

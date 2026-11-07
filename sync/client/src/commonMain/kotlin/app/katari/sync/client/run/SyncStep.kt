package app.katari.sync.client.run

/**
 * A step of the run under way that can take a while, [done] of [total] so far. A first download or full resync is not
 * one: its stage and position say how far it got ([app.katari.sync.client.connection.SyncConnectionState.stageTarget]).
 */
data class SyncStep(val kind: Kind, val done: Long, val total: Long) {
    enum class Kind {
        /** Changes made here going to the server, counted in records, such as a whole library after signing in. */
        UPLOADING_RECORDS,

        /** Custom covers and fonts the server lacks, counted in files. */
        UPLOADING_FILES,

        /** Custom covers and fonts the records name that aren't here yet, counted in files. */
        DOWNLOADING_FILES,

        /** Other devices' changes since this device's position, counted in server sequences. */
        DOWNLOADING_CHANGES,
    }
}

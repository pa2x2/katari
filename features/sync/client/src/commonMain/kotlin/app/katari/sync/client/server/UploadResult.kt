package app.katari.sync.client.server

/** How the server answered a file upload ([SyncServerClient.uploadFile]). */
enum class UploadResult {
    STORED,

    /** The bytes don't hash to the file's name: the file changed while it was read. */
    CHANGED,

    /** Too large for the server, or for a proxy in front of it, which no later upload of the same file changes. */
    TOO_LARGE,
}

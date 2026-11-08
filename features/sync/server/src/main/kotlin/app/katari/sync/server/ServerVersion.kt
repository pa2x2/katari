package app.katari.sync.server

/** The version the build stamped into the server's jar; a server run from classes, as in tests, has none. */
internal object ServerVersion {
    val name: String = ServerVersion::class.java.`package`?.implementationVersion ?: "development"
}

package app.katari.sync.server

import app.katari.sync.server.account.PasswordHasher
import app.katari.sync.server.config.ServerConfig
import app.katari.sync.server.recovery.PasswordReset
import app.katari.sync.server.storage.sqlite.SqliteServerStorage
import java.security.SecureRandom
import kotlin.io.path.exists
import kotlin.system.exitProcess

private const val USAGE = """Usage:
  katari-sync-server [serve]                 Serve the sync API
  katari-sync-server reset-password <user>   Give an account a new random password and enable it

Configured by the environment variables ${ServerConfig.DATA_DIRECTORY}, ${ServerConfig.HOST}, ${ServerConfig.PORT},
${ServerConfig.MAX_REQUEST_BYTES}, ${ServerConfig.MAX_FILE_BYTES} and ${ServerConfig.PULL_PAGE_SIZE}."""

fun main(args: Array<String>) {
    val config = try {
        ServerConfig.fromEnvironment()
    } catch (e: IllegalArgumentException) {
        exit(e.message.orEmpty())
    }
    when {
        args.isEmpty() || args.contentEquals(arrayOf("serve")) -> serve(config)
        args.size == 2 && args[0] == "reset-password" -> resetPassword(config, args[1])
        else -> exit(USAGE)
    }
}

private fun resetPassword(config: ServerConfig, username: String) {
    if (!config.databaseFile.exists()) exit("No server database at ${config.databaseFile}")
    val random = SecureRandom()
    val password = SqliteServerStorage.open(config.databaseFile).use { storage ->
        PasswordReset(storage, PasswordHasher(random = random), random).reset(username)
    } ?: exit("No account is named $username")
    println("The new password of $username is $password")
}

private fun exit(message: String): Nothing {
    System.err.println(message)
    exitProcess(2)
}

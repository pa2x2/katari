package app.katari.showcase.sync

import app.katari.sync.server.SyncServer
import app.katari.sync.server.account.FirstAdminOutcome
import app.katari.sync.server.administration.AdminOutcome
import app.katari.sync.server.config.ServerConfig
import java.security.SecureRandom
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * The sync server as the showcase runs it: the real server, configured by the same environment variables, whose clock
 * stands still at the showcase moment and whose random numbers come from a fixed seed. What it shows then reads the
 * same on every run: devices that synced "just now", link codes that expire two minutes after the moment, and the same
 * codes. A clock that ran on would have the account list say "3 minutes ago" on one run and "4 minutes ago" on the next.
 *
 * Arguments: the showcase moment in epoch milliseconds, then a username and a password for each account it creates
 * before serving, the admin's first.
 */
fun main(args: Array<String>) {
    require(args.size >= 3 && args.size % 2 == 1) {
        "Usage: showcase-sync-server <moment> <admin> <password> [<username> <password>]..."
    }
    val moment = Instant.fromEpochMilliseconds(args[0].toLong())
    val server = SyncServer(
        ServerConfig.fromEnvironment(),
        clock = object : Clock {
            override fun now(): Instant = moment
        },
        // SHA1PRNG seeded before its first use draws the same numbers on every JVM.
        random = SecureRandom.getInstance("SHA1PRNG").apply { setSeed(SEED) },
    )
    createAccounts(server, args.drop(1).chunked(2).map { (username, password) -> username to password })
    server.start(wait = true)
}

private fun createAccounts(server: SyncServer, accounts: List<Pair<String, String>>) {
    val services = server.services
    val setupCode = checkNotNull(services.firstAdminSetup.setupCode) { "The data directory already has accounts" }
    val (adminName, adminPassword) = accounts.first()
    val created = services.firstAdminSetup.createAdmin(setupCode, adminName, adminPassword, LOOPBACK)
    val admin =
        (created as? FirstAdminOutcome.Created)?.account ?: error("The admin $adminName wasn't created: $created")
    for ((username, password) in accounts.drop(1)) {
        val outcome = services.administration.add(admin.id, username, password)
        check(outcome is AdminOutcome.Done) { "The account $username wasn't created: $outcome" }
    }
}

private val SEED = "katari-showcase".encodeToByteArray()

private const val LOOPBACK = "127.0.0.1"

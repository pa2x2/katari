package app.katari.sync.server.administration

import app.katari.sync.protocol.account.AccountCredentials
import app.katari.sync.protocol.device.DeviceId
import app.katari.sync.server.account.Account
import app.katari.sync.server.account.AccountSummary
import app.katari.sync.server.account.GeneratedPasswords
import app.katari.sync.server.account.PasswordHasher
import app.katari.sync.server.file.FileContentStore
import app.katari.sync.server.live.AccountChanges
import app.katari.sync.server.storage.ServerStorage
import java.security.SecureRandom
import kotlin.time.Clock

/**
 * What admins do to accounts on the web page. Every action names the acting account, which must be an enabled admin
 * when the action runs ([enabledAdmin]), and none may leave the server without an enabled admin: the last one cannot be
 * demoted, disabled or deleted. Actions that take away a sign-in end the account's web sessions; disabling keeps its
 * devices linked but refused until it is enabled again.
 */
class AccountAdministration(
    private val storage: ServerStorage,
    private val passwords: PasswordHasher,
    private val content: FileContentStore,
    private val changes: AccountChanges,
    private val clock: Clock,
    private val random: SecureRandom,
) {
    fun accounts(actorId: Long): AdminOutcome<List<AccountSummary>> = storage.transaction {
        storage.enabledAdmin(actorId) ?: return@transaction AdminOutcome.NotAllowed
        AdminOutcome.Done(storage.accounts.summaries())
    }

    fun account(actorId: Long, accountId: Long): AdminOutcome<ManagedAccount> = storage.transaction {
        storage.enabledAdmin(actorId) ?: return@transaction AdminOutcome.NotAllowed
        val summary = storage.accounts.summary(accountId) ?: return@transaction AdminOutcome.NoSuchAccount
        val isLastEnabledAdmin = summary.isAdmin && !summary.isDisabled && storage.accounts.countEnabledAdmins() <= 1
        AdminOutcome.Done(ManagedAccount(summary, isLastEnabledAdmin, storage.devices.activeIn(accountId)))
    }

    fun add(actorId: Long, username: String, password: String): AdminOutcome<Account> {
        val name = username.trim()
        if (!AccountCredentials.isValidUsername(name)) return AdminOutcome.InvalidUsername
        if (!AccountCredentials.isValidPassword(password)) return AdminOutcome.InvalidPassword
        // Hashing is slow, so it runs before the transaction rather than holding the database's write lock.
        val hash = passwords.hash(password)
        return storage.transaction {
            storage.enabledAdmin(actorId) ?: return@transaction AdminOutcome.NotAllowed
            if (storage.accounts.byUsername(name) != null) return@transaction AdminOutcome.UsernameTaken
            val created = storage.accounts.create(
                name,
                hash,
                isAdmin = false,
                createdAtMillis = clock.now().toEpochMilliseconds(),
            )
            AdminOutcome.Done(created)
        }
    }

    /** Gives the account a new random password, which is returned to be shown once, and signs it out of the web. */
    fun resetPassword(actorId: Long, accountId: Long): AdminOutcome<String> {
        val password = GeneratedPasswords.generate(random)
        val hash = passwords.hash(password)
        return change(actorId, accountId) { account ->
            storage.accounts.setPasswordHash(account.id, hash)
            storage.webSessions.deleteForAccount(account.id)
            AdminOutcome.Done(password)
        }
    }

    fun setAdmin(actorId: Long, accountId: Long, admin: Boolean): AdminOutcome<Unit> =
        change(actorId, accountId) { account ->
            if (!admin && account.isLastEnabledAdmin()) return@change AdminOutcome.LastAdmin
            storage.accounts.setAdmin(account.id, admin)
            AdminOutcome.Done(Unit)
        }

    fun setDisabled(actorId: Long, accountId: Long, disabled: Boolean): AdminOutcome<Unit> =
        change(actorId, accountId) { account ->
            if (disabled && account.isLastEnabledAdmin()) return@change AdminOutcome.LastAdmin
            storage.accounts.setDisabled(account.id, disabled)
            if (disabled) storage.webSessions.deleteForAccount(account.id)
            AdminOutcome.Done(Unit)
        }

    /**
     * Removes the account with its records, files and devices once [typedUsername] confirms which account it is. Its
     * devices are answered as revoked from then on, so they carry on standalone with everything they hold.
     */
    fun delete(actorId: Long, accountId: Long, typedUsername: String): AdminOutcome<Unit> {
        var devices = emptyList<DeviceId>()
        val outcome = change(actorId, accountId) { account ->
            if (!typedUsername.trim().equals(account.username, ignoreCase = true)) {
                return@change AdminOutcome.NotConfirmed
            }
            if (account.isLastEnabledAdmin()) return@change AdminOutcome.LastAdmin
            devices = storage.devices.activeIn(account.id).map { it.id }
            storage.devices.retireAll(account.id)
            storage.accounts.delete(account.id)
            AdminOutcome.Done(Unit)
        }
        if (outcome is AdminOutcome.Done) {
            // After the commit: the account's id is never given out again, so a crash before this leaves only an
            // orphaned folder, never one another account would read.
            content.deleteAccount(accountId)
            devices.forEach(changes::announceRevoked)
        }
        return outcome
    }

    private inline fun <T> change(
        actorId: Long,
        accountId: Long,
        crossinline action: (Account) -> AdminOutcome<T>,
    ): AdminOutcome<T> = storage.transaction {
        storage.enabledAdmin(actorId) ?: return@transaction AdminOutcome.NotAllowed
        val account = storage.accounts.byId(accountId) ?: return@transaction AdminOutcome.NoSuchAccount
        action(account)
    }

    private fun Account.isLastEnabledAdmin(): Boolean =
        isAdmin && !isDisabled && storage.accounts.countEnabledAdmins() <= 1
}

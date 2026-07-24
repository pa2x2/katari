package mihon.entry.interactions

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import mihon.entry.interactions.host.tracking.EntryTrackingHost
import mihon.entry.interactions.host.tracking.EntryTrackingHostAccount
import mihon.entry.interactions.host.tracking.EntryTrackingHostCredentialIdentity
import mihon.entry.interactions.host.tracking.EntryTrackingHostLoginMethod

internal class DefaultEntryTrackingAccounts(
    private val host: EntryTrackingHost,
) : EntryTrackingAccounts {
    override fun currentAccounts(): EntryTrackingAccountSnapshot {
        return EntryTrackingAccountSnapshot(host.accounts.currentAccounts().map(EntryTrackingHostAccount::toAccount))
    }

    override fun observeAccounts(): Flow<EntryTrackingAccountSnapshot> {
        return host.accounts.observeAccounts()
            .map { accounts -> EntryTrackingAccountSnapshot(accounts.map(EntryTrackingHostAccount::toAccount)) }
            .distinctUntilChanged()
    }

    override fun storedCredentials(serviceId: EntryTrackingServiceId): EntryTrackingStoredCredentials? {
        val account = account(serviceId) ?: return null
        if (account.loginMethod !is EntryTrackingHostLoginMethod.Credentials) return null
        return host.accounts.storedCredentials(serviceId.value).let { credentials ->
            EntryTrackingStoredCredentials(credentials.username, credentials.password)
        }
    }

    override suspend fun beginLogin(serviceId: EntryTrackingServiceId): EntryTrackingAccountOperationResult {
        val account = account(serviceId) ?: return serviceNotRegistered()
        return when (account.loginMethod) {
            EntryTrackingHostLoginMethod.External -> accountCatching {
                EntryTrackingAccountOperationResult.ExternalAuthorization(
                    host.accounts.beginExternalLogin(serviceId.value),
                )
            }
            is EntryTrackingHostLoginMethod.Credentials -> loginMethodMismatch()
            EntryTrackingHostLoginMethod.Passive -> accountCatching {
                host.accounts.loginPassively(serviceId.value)
                EntryTrackingAccountOperationResult.Completed
            }
        }
    }

    override suspend fun loginWithCredentials(
        serviceId: EntryTrackingServiceId,
        username: String,
        password: String,
    ): EntryTrackingAccountOperationResult {
        val account = account(serviceId) ?: return serviceNotRegistered()
        if (account.loginMethod !is EntryTrackingHostLoginMethod.Credentials) return loginMethodMismatch()
        return accountCatching {
            host.accounts.loginWithCredentials(serviceId.value, username, password)
            EntryTrackingAccountOperationResult.Completed
        }
    }

    override suspend fun logout(serviceId: EntryTrackingServiceId): EntryTrackingAccountOperationResult {
        account(serviceId) ?: return serviceNotRegistered()
        return accountCatching {
            host.accounts.logout(serviceId.value)
            EntryTrackingAccountOperationResult.Completed
        }
    }

    override fun missingLoginNames(serviceIds: Set<EntryTrackingServiceId>): List<String> {
        val requested = serviceIds.mapTo(mutableSetOf(), EntryTrackingServiceId::value)
        return host.accounts.currentAccounts()
            .filter { it.service.id in requested && !it.isLoggedIn }
            .map { it.service.name }
            .sorted()
    }

    private fun account(serviceId: EntryTrackingServiceId): EntryTrackingHostAccount? {
        return host.accounts.currentAccounts().firstOrNull { it.service.id == serviceId.value }
    }
}

private fun EntryTrackingHostAccount.toAccount() = EntryTrackingAccount(
    service = service.toDescriptor(),
    loginMethod = when (val method = loginMethod) {
        EntryTrackingHostLoginMethod.External -> EntryTrackingLoginMethod.External
        is EntryTrackingHostLoginMethod.Credentials -> EntryTrackingLoginMethod.Credentials(
            when (method.identity) {
                EntryTrackingHostCredentialIdentity.USERNAME -> EntryTrackingCredentialIdentity.USERNAME
                EntryTrackingHostCredentialIdentity.EMAIL -> EntryTrackingCredentialIdentity.EMAIL
            },
        )
        EntryTrackingHostLoginMethod.Passive -> EntryTrackingLoginMethod.Passive
    },
    isLoggedIn = isLoggedIn,
    displayUsername = displayUsername,
    isAvailable = isAvailable,
)

private inline fun accountCatching(
    block: () -> EntryTrackingAccountOperationResult,
): EntryTrackingAccountOperationResult {
    return try {
        block()
    } catch (error: Throwable) {
        if (error is CancellationException) throw error
        EntryTrackingAccountOperationResult.Failed(error)
    }
}

private fun serviceNotRegistered() = EntryTrackingAccountOperationResult.Unavailable(
    EntryTrackingAccountUnavailableReason.SERVICE_NOT_REGISTERED,
)

private fun loginMethodMismatch() = EntryTrackingAccountOperationResult.Unavailable(
    EntryTrackingAccountUnavailableReason.LOGIN_METHOD_MISMATCH,
)

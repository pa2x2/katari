package mihon.entry.interactions

import kotlinx.coroutines.flow.Flow

interface EntryTrackingAccounts {
    fun currentAccounts(): EntryTrackingAccountSnapshot

    fun observeAccounts(): Flow<EntryTrackingAccountSnapshot>

    fun storedCredentials(serviceId: EntryTrackingServiceId): EntryTrackingStoredCredentials?

    suspend fun beginLogin(serviceId: EntryTrackingServiceId): EntryTrackingAccountOperationResult

    suspend fun loginWithCredentials(
        serviceId: EntryTrackingServiceId,
        username: String,
        password: String,
    ): EntryTrackingAccountOperationResult

    suspend fun logout(serviceId: EntryTrackingServiceId): EntryTrackingAccountOperationResult

    fun missingLoginNames(serviceIds: Set<EntryTrackingServiceId>): List<String>
}

data class EntryTrackingAccountSnapshot(
    val accounts: List<EntryTrackingAccount>,
)

data class EntryTrackingAccount(
    val service: EntryTrackingServiceDescriptor,
    val loginMethod: EntryTrackingLoginMethod,
    val isLoggedIn: Boolean,
    val displayUsername: String,
    val isAvailable: Boolean,
)

data class EntryTrackingStoredCredentials(
    val username: String,
    val password: String,
)

sealed interface EntryTrackingLoginMethod {
    data object External : EntryTrackingLoginMethod

    data class Credentials(
        val identity: EntryTrackingCredentialIdentity,
    ) : EntryTrackingLoginMethod

    data object Passive : EntryTrackingLoginMethod
}

enum class EntryTrackingCredentialIdentity {
    USERNAME,
    EMAIL,
}

sealed interface EntryTrackingAccountOperationResult {
    data object Completed : EntryTrackingAccountOperationResult

    data class ExternalAuthorization(
        val uri: String,
    ) : EntryTrackingAccountOperationResult

    data class Unavailable(
        val reason: EntryTrackingAccountUnavailableReason,
    ) : EntryTrackingAccountOperationResult

    data class Failed(
        val cause: Throwable,
    ) : EntryTrackingAccountOperationResult
}

enum class EntryTrackingAccountUnavailableReason {
    SERVICE_NOT_REGISTERED,
    LOGIN_METHOD_MISMATCH,
}

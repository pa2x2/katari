package mihon.entry.interactions.host.tracking

import kotlinx.coroutines.flow.Flow

interface EntryTrackingAccountHost {
    fun currentAccounts(): List<EntryTrackingHostAccount>

    fun observeAccounts(): Flow<List<EntryTrackingHostAccount>>

    fun storedCredentials(serviceId: Long): EntryTrackingHostStoredCredentials

    suspend fun beginExternalLogin(serviceId: Long): String

    suspend fun loginWithCredentials(serviceId: Long, username: String, password: String)

    suspend fun loginPassively(serviceId: Long)

    suspend fun logout(serviceId: Long)
}

data class EntryTrackingHostAccount(
    val service: EntryTrackingHostService,
    val loginMethod: EntryTrackingHostLoginMethod,
    val isLoggedIn: Boolean,
    val displayUsername: String,
    val isAvailable: Boolean,
)

data class EntryTrackingHostStoredCredentials(
    val username: String,
    val password: String,
)

sealed interface EntryTrackingHostLoginMethod {
    data object External : EntryTrackingHostLoginMethod

    data class Credentials(
        val identity: EntryTrackingHostCredentialIdentity,
    ) : EntryTrackingHostLoginMethod

    data object Passive : EntryTrackingHostLoginMethod
}

enum class EntryTrackingHostCredentialIdentity {
    USERNAME,
    EMAIL,
}

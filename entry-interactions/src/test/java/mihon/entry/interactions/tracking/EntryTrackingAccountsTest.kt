package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.host.tracking.EntryTrackingAccountHost
import mihon.entry.interactions.host.tracking.EntryTrackingAutomationHost
import mihon.entry.interactions.host.tracking.EntryTrackingBackupHost
import mihon.entry.interactions.host.tracking.EntryTrackingCollectionHost
import mihon.entry.interactions.host.tracking.EntryTrackingHost
import mihon.entry.interactions.host.tracking.EntryTrackingHostAccount
import mihon.entry.interactions.host.tracking.EntryTrackingHostCredentialIdentity
import mihon.entry.interactions.host.tracking.EntryTrackingHostEntrySnapshot
import mihon.entry.interactions.host.tracking.EntryTrackingHostLoginMethod
import mihon.entry.interactions.host.tracking.EntryTrackingHostService
import mihon.entry.interactions.host.tracking.EntryTrackingHostServiceCapabilities
import mihon.entry.interactions.host.tracking.EntryTrackingHostStoredCredentials
import mihon.entry.interactions.host.tracking.EntryTrackingOperationHost
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry

class EntryTrackingAccountsTest {
    private val external = account(7L, "External", EntryTrackingHostLoginMethod.External, loggedIn = false)
    private val credentials = account(
        8L,
        "Credentials",
        EntryTrackingHostLoginMethod.Credentials(EntryTrackingHostCredentialIdentity.EMAIL),
        loggedIn = false,
    )
    private val loggedIn = account(9L, "Logged in", EntryTrackingHostLoginMethod.Passive, loggedIn = true)

    @Test
    fun `authoritative account snapshot projects every registered service and login method`() {
        val accounts = mockk<EntryTrackingAccountHost>()
        every { accounts.currentAccounts() } returns listOf(external, credentials, loggedIn)
        val feature = feature(accounts)

        feature.currentAccounts().accounts.map { account -> account.service.name to account.loginMethod } shouldBe
            listOf(
                "External" to EntryTrackingLoginMethod.External,
                "Credentials" to EntryTrackingLoginMethod.Credentials(EntryTrackingCredentialIdentity.EMAIL),
                "Logged in" to EntryTrackingLoginMethod.Passive,
            )
    }

    @Test
    fun `account commands validate login method before host dispatch`() = runTest {
        val accounts = mockk<EntryTrackingAccountHost>()
        every { accounts.currentAccounts() } returns listOf(external, credentials)
        coEvery { accounts.beginExternalLogin(external.service.id) } returns "https://example.com/login"
        coEvery { accounts.loginWithCredentials(credentials.service.id, "mail", "secret") } returns Unit
        val feature = feature(accounts)

        feature.beginLogin(EntryTrackingServiceId(external.service.id)) shouldBe
            EntryTrackingAccountOperationResult.ExternalAuthorization("https://example.com/login")
        feature.loginWithCredentials(
            EntryTrackingServiceId(credentials.service.id),
            "mail",
            "secret",
        ) shouldBe EntryTrackingAccountOperationResult.Completed
        feature.loginWithCredentials(
            EntryTrackingServiceId(external.service.id),
            "mail",
            "secret",
        ) shouldBe EntryTrackingAccountOperationResult.Unavailable(
            EntryTrackingAccountUnavailableReason.LOGIN_METHOD_MISMATCH,
        )

        coVerify(exactly = 1) { accounts.beginExternalLogin(external.service.id) }
        coVerify(exactly = 1) { accounts.loginWithCredentials(credentials.service.id, "mail", "secret") }
        coVerify(exactly = 0) { accounts.loginWithCredentials(external.service.id, any(), any()) }
    }

    @Test
    fun `stored credentials are exposed only for credential login`() {
        val accounts = mockk<EntryTrackingAccountHost>()
        every { accounts.currentAccounts() } returns listOf(external, credentials)
        every { accounts.storedCredentials(credentials.service.id) } returns
            EntryTrackingHostStoredCredentials("mail", "secret")
        val feature = feature(accounts)

        feature.storedCredentials(EntryTrackingServiceId(credentials.service.id)) shouldBe
            EntryTrackingStoredCredentials("mail", "secret")
        feature.storedCredentials(EntryTrackingServiceId(external.service.id)) shouldBe null

        io.mockk.verify(exactly = 1) { accounts.storedCredentials(credentials.service.id) }
        io.mockk.verify(exactly = 0) { accounts.storedCredentials(external.service.id) }
    }

    @Test
    fun `backup diagnostics resolve only registered logged out services`() {
        val accounts = mockk<EntryTrackingAccountHost>()
        every { accounts.currentAccounts() } returns listOf(external, credentials, loggedIn)
        val feature = feature(accounts)

        feature.missingLoginNames(
            setOf(
                EntryTrackingServiceId(loggedIn.service.id),
                EntryTrackingServiceId(credentials.service.id),
                EntryTrackingServiceId(external.service.id),
                EntryTrackingServiceId(404L),
            ),
        ) shouldBe listOf("Credentials", "External")
    }

    private fun feature(accountHost: EntryTrackingAccountHost): EntryTrackingFeature {
        val host = object : EntryTrackingHost {
            override val operations: EntryTrackingOperationHost = mockk(relaxed = true)
            override val automation: EntryTrackingAutomationHost = mockk(relaxed = true)
            override val accounts = accountHost
            override val collection: EntryTrackingCollectionHost = mockk(relaxed = true)
            override val backup: EntryTrackingBackupHost = EntryTrackingBackupHost.Empty

            override fun registeredServices() = accountHost.currentAccounts().map(EntryTrackingHostAccount::service)

            override fun observeEntry(entry: Entry) = flowOf(EntryTrackingHostEntrySnapshot(emptyList()))
        }
        return DefaultEntryTrackingFeature(
            evaluation = sourceFeatureEvaluation(EntryTrackingFeatureContributor),
            host = host,
        )
    }

    private fun account(
        id: Long,
        name: String,
        loginMethod: EntryTrackingHostLoginMethod,
        loggedIn: Boolean,
    ) = EntryTrackingHostAccount(
        service = EntryTrackingHostService(
            id = id,
            name = name,
            logoResource = 0,
            supportedEntryTypes = setOf(EntryType.BOOK),
            capabilities = EntryTrackingHostServiceCapabilities(
                statuses = emptyList(),
                scores = emptyList(),
                supportsReadingDates = false,
                supportsPrivateTracking = false,
                supportsRemoteDeletion = false,
                supportsAutomaticBinding = false,
            ),
        ),
        loginMethod = loginMethod,
        isLoggedIn = loggedIn,
        displayUsername = name,
        isAvailable = true,
    )
}

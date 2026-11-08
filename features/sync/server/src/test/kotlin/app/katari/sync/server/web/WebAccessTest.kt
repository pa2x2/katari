package app.katari.sync.server.web

import app.katari.sync.server.TestServer
import app.katari.sync.server.device.AuthenticationOutcome
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.HttpClient
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.parameters
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

/**
 * The web page's two guards on everything that changes state: admin actions only for admins, and every POST only
 * with the form token of the visitor sending it. Both are checked through HTTP against a real database, since what
 * protects is that no route can be reached around them.
 */
class WebAccessTest {

    @Test
    fun `an account that isn't an admin is refused every admin page and action`(@TempDir dataDirectory: Path) =
        webTest(dataDirectory) { server, ids ->
            val anna = browser().apply { signIn("anna", ANNA_PASSWORD) }
            val token = anna.formToken("/account")
            val before = server.accountState()

            adminPages(ids).forEach { anna.get(it).status shouldBe HttpStatusCode.Forbidden }
            adminActions(ids).forEach { (path, fields) ->
                val response = anna.post(path, token, fields)
                response.status shouldBe HttpStatusCode.Forbidden
                response.bodyAsText() shouldNotContain "Invite ready"
            }
            server.accountState() shouldBe before
        }

    @Test
    fun `a POST without the sender's own form token changes nothing`(@TempDir dataDirectory: Path) =
        webTest(dataDirectory) { server, ids ->
            val kirylDevice = server.device(ids.kiryl)
            val kiryl = browser().apply { signIn("kiryl", KIRYL_PASSWORD) }
            val annasToken = browser().apply { signIn("anna", ANNA_PASSWORD) }.formToken("/account")
            val before = server.accountState()
            val deviceId = kirylDevice.device.id.value
            val actions = adminActions(ids) + listOf(
                "/account/devices/$deviceId/remove" to emptyMap(),
                "/account/password" to mapOf("current" to KIRYL_PASSWORD, "password" to NEW, "repeat" to NEW),
                "/account/synced-data" to mapOf("password" to KIRYL_PASSWORD),
                "/sign-out" to emptyMap(),
            )

            actions.forEach { (path, fields) ->
                listOf(null, annasToken).forEach { token ->
                    val response = kiryl.post(path, token, fields)
                    response.status shouldBe HttpStatusCode.Forbidden
                    response.bodyAsText() shouldNotContain "Invite ready"
                }
            }
            val signIn = mapOf("username" to "anna", "password" to ANNA_PASSWORD)
            browser().post("/sign-in", token = null, signIn).headers["Set-Cookie"].orEmpty() shouldNotContain "session"

            server.accountState() shouldBe before
            server.services.authentication.authenticate(kirylDevice.token)
                .shouldBeInstanceOf<AuthenticationOutcome.Authenticated>()
            kiryl.get("/account").status shouldBe HttpStatusCode.OK
        }

    private class Accounts(val kiryl: Long, val anna: Long)

    private fun webTest(dataDirectory: Path, test: suspend ApplicationTestBuilder.(TestServer, Accounts) -> Unit) {
        TestServer(dataDirectory).use { server ->
            val setup = server.services.firstAdminSetup
            setup.createAdmin(checkNotNull(setup.setupCode), "kiryl", KIRYL_PASSWORD, remoteAddress = "192.0.2.1")
            val ids = Accounts(
                kiryl = checkNotNull(server.services.storage.accounts.byUsername("kiryl")).id,
                anna = server.account("anna", ANNA_PASSWORD),
            )
            testApplication {
                application { webPages(server.services) }
                test(server, ids)
            }
        }
    }

    private fun adminPages(ids: Accounts) =
        listOf("/accounts/new", "/accounts/${ids.kiryl}", "/accounts/${ids.kiryl}/delete")

    private fun adminActions(ids: Accounts): List<Pair<String, Map<String, String>>> = listOf(
        "/accounts/new" to mapOf("username" to "intruder", "password" to NEW, "repeat" to NEW),
        "/invites" to emptyMap(),
        "/accounts/${ids.kiryl}/password-reset" to emptyMap(),
        "/accounts/${ids.kiryl}/admin" to mapOf("admin" to "false"),
        "/accounts/${ids.anna}/admin" to mapOf("admin" to "true"),
        "/accounts/${ids.kiryl}/disabled" to mapOf("disabled" to "true"),
        "/accounts/${ids.kiryl}/delete" to mapOf("username" to "kiryl"),
    )

    /** Everything an action could change, password hashes included. */
    private fun TestServer.accountState() = services.storage.accounts.summaries().map {
        it to services.storage.accounts.byId(it.id)?.passwordHash
    }

    private fun ApplicationTestBuilder.browser(): HttpClient = createClient {
        install(HttpCookies)
        followRedirects = false
    }

    private suspend fun HttpClient.signIn(username: String, password: String) {
        val response = post("/sign-in", formToken("/sign-in"), mapOf("username" to username, "password" to password))
        response.status shouldBe HttpStatusCode.SeeOther
    }

    private suspend fun HttpClient.formToken(page: String): String =
        checkNotNull(formTokenField.find(get(page).bodyAsText())).groupValues[1]

    private suspend fun HttpClient.post(path: String, token: String?, fields: Map<String, String>) = submitForm(
        path,
        parameters {
            token?.let { append("form_token", it) }
            fields.forEach { (name, value) -> append(name, value) }
        },
    )

    private companion object {
        const val KIRYL_PASSWORD = "kiryl's password"
        const val ANNA_PASSWORD = "anna's password"
        const val NEW = "a new password"
        val formTokenField = Regex("""name="form_token" value="([^"]+)"""")
    }
}

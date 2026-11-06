package app.katari.sync.server.administration

import app.katari.sync.server.TestServer
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class AccountAdministrationTest {

    @Test
    fun `the last enabled admin can't be demoted, disabled or deleted, even beside a disabled admin`(
        @TempDir dataDirectory: Path,
    ) {
        TestServer(dataDirectory).use { server ->
            val administration = server.services.administration
            val kiryl = server.account("kiryl", isAdmin = true)
            val anna = server.account("anna", isAdmin = true)
            administration.setDisabled(kiryl, anna, disabled = true).shouldBeInstanceOf<AdminOutcome.Done<Unit>>()

            administration.setAdmin(kiryl, kiryl, admin = false) shouldBe AdminOutcome.LastAdmin
            administration.setDisabled(kiryl, kiryl, disabled = true) shouldBe AdminOutcome.LastAdmin
            administration.delete(kiryl, kiryl, typedUsername = "kiryl") shouldBe AdminOutcome.LastAdmin
            server.services.storage.accounts.byId(kiryl)?.run { isAdmin && !isDisabled } shouldBe true

            administration.setDisabled(kiryl, anna, disabled = false).shouldBeInstanceOf<AdminOutcome.Done<Unit>>()
            administration.setAdmin(anna, kiryl, admin = false).shouldBeInstanceOf<AdminOutcome.Done<Unit>>()
            // Demoted while a page of theirs was still open: nothing they send is allowed any more.
            administration.setAdmin(kiryl, kiryl, admin = true) shouldBe AdminOutcome.NotAllowed
            administration.setAdmin(anna, anna, admin = false) shouldBe AdminOutcome.LastAdmin
        }
    }
}

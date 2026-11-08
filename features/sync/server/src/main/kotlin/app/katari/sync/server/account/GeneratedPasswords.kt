package app.katari.sync.server.account

import app.katari.sync.server.onetimecode.OneTimeCode
import java.security.SecureRandom

/**
 * Passwords the server makes up when one is reset, by the recovery command or an admin on the web page. Each is shown
 * once and typed once, before its owner picks their own.
 */
internal object GeneratedPasswords {
    // 80 bits, in the grouped form people read off a screen.
    private const val LENGTH = 16

    fun generate(random: SecureRandom): String = OneTimeCode.generate(LENGTH, random)
}

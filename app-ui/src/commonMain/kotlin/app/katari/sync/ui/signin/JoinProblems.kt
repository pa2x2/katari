package app.katari.sync.ui.signin

import androidx.compose.runtime.Composable
import app.katari.sync.client.account.JoinResult
import app.katari.sync.client.account.ServerCheck
import app.katari.sync.client.server.Refusal
import app.katari.sync.protocol.version.ProtocolCompatibility
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.math.ceil

/** How a device was joining an account, which decides how a refusal of what it sent is worded. */
enum class JoinPath { PASSWORD, LINK_CODE, INVITE }

/**
 * Why joining failed, in words (D027, D031, D061); null for a result that isn't a failure. Every refusal says why,
 * since the server tells nothing more than its error code and the person has to know what to change.
 */
@Composable
fun joinProblemText(result: JoinResult, path: JoinPath): String? = when (result) {
    is JoinResult.Joined -> null
    is JoinResult.ServerNotReady -> serverCheckProblemText(result.check)
    JoinResult.Unreachable -> stringResource(MR.strings.sync_join_unreachable)
    JoinResult.NoSecretStore -> stringResource(MR.strings.sync_no_keyring)
    is JoinResult.Refused -> when (val reason = result.reason) {
        Refusal.NotAccepted, Refusal.TokenNotAccepted -> stringResource(
            when (path) {
                JoinPath.PASSWORD -> MR.strings.sync_refused_credentials
                JoinPath.LINK_CODE -> MR.strings.sync_refused_link_code
                JoinPath.INVITE -> MR.strings.sync_refused_invite
            },
        )
        is Refusal.Throttled -> {
            val minutes = ceil(reason.retryAfter.inWholeSeconds / 60.0).toInt().coerceAtLeast(1)
            stringResource(
                MR.strings.sync_refused_throttled,
                pluralStringResource(MR.plurals.sync_minutes, minutes, minutes),
            )
        }
        Refusal.UsernameTaken -> stringResource(MR.strings.sync_refused_username_taken)
        Refusal.AccountDisabled -> stringResource(MR.strings.sync_refused_account_disabled)
        is Refusal.Incompatible -> compatibilityText(reason.compatibility)
        Refusal.DeviceRemoved, Refusal.PositionExpired, Refusal.PositionUnknown ->
            stringResource(MR.strings.sync_refused_other)
    }
}

/** What checking a server address found, in words; null when the server is ready and nothing needs saying. */
@Composable
fun serverCheckProblemText(check: ServerCheck): String? = when (check) {
    is ServerCheck.Ready -> null
    is ServerCheck.NeedsSetup -> stringResource(MR.strings.sync_server_needs_setup, check.webPage.toString())
    is ServerCheck.Incompatible -> compatibilityText(check.compatibility)
    ServerCheck.Malformed -> stringResource(MR.strings.sync_server_malformed)
    ServerCheck.PlainHttpOutsideLocalNetwork -> stringResource(MR.strings.sync_server_plain_http_refused)
    ServerCheck.Unreachable -> stringResource(MR.strings.sync_server_unreachable)
    ServerCheck.NotASyncServer -> stringResource(MR.strings.sync_server_not_sync)
}

@Composable
private fun compatibilityText(compatibility: ProtocolCompatibility): String = stringResource(
    when (compatibility) {
        ProtocolCompatibility.APP_TOO_OLD -> MR.strings.sync_server_needs_newer_app
        ProtocolCompatibility.SERVER_TOO_OLD, ProtocolCompatibility.SUPPORTED -> MR.strings.sync_server_needs_update
    },
)

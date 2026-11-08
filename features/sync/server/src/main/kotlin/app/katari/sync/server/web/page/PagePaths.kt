package app.katari.sync.server.web.page

internal const val SIGN_IN_PATH = "/sign-in"
internal const val SIGN_OUT_PATH = "/sign-out"
internal const val ACCOUNT_PATH = "/account"
internal const val ACCOUNT_PASSWORD_PATH = "/account/password"
internal const val ACCOUNT_SYNCED_DATA_PATH = "/account/synced-data"
internal const val NEW_ACCOUNT_PATH = "/accounts/new"
internal const val INVITES_PATH = "/invites"

internal fun ownDeviceRemovalPath(deviceId: Long): String = "/account/devices/$deviceId/remove"

internal fun managedAccountPath(accountId: Long): String = "/accounts/$accountId"

internal fun managedAccountActionPath(accountId: Long, action: ManagedAccountAction): String =
    "${managedAccountPath(accountId)}/${action.segment}"

internal enum class ManagedAccountAction(val segment: String) {
    ResetPassword("password-reset"),
    SetAdmin("admin"),
    SetDisabled("disabled"),
    Delete("delete"),
}

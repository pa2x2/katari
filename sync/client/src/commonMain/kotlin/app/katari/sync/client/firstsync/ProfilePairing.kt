package app.katari.sync.client.firstsync

import app.katari.sync.client.kind.profile.ProfileKind

/** A profile the account already has, from its record. */
data class AccountProfile(val uuid: String, val name: String)

/** One of this device's profiles, and the account profile proposed as the same person's; null to add it as new. */
data class ProfilePairing(val profileId: Long, val name: String, val pairedWith: AccountProfile?)

/** Whose person-level settings a paired profile keeps where both have a value. */
enum class SettingsWinner { ACCOUNT, DEVICE }

/**
 * The person's answer for one of this device's profiles: the account profile it is ([accountUuid]), or null to add
 * it to the account as a new one, and, for a paired profile, whose settings win.
 */
data class ProfileChoice(
    val profileId: Long,
    val accountUuid: String?,
    val settings: SettingsWinner = SettingsWinner.ACCOUNT,
)

/**
 * Pairs [local] profiles with [account] ones by name, ignoring case and surrounding spaces, each account profile with
 * one device profile at most. The default profile pairs by itself, with the account's default, whatever either is
 * called.
 */
internal fun pairByName(local: List<LocalProfile>, account: List<AccountProfile>): List<ProfilePairing> {
    val unpaired = account.filterNot { it.uuid == ProfileKind.DEFAULT_UUID }.toMutableList()
    val accountDefault = account.firstOrNull { it.uuid == ProfileKind.DEFAULT_UUID }
    return local.map { profile ->
        val pairedWith = if (profile.uuid == ProfileKind.DEFAULT_UUID) {
            accountDefault
        } else {
            unpaired.firstOrNull { it.name.trim().equals(profile.name.trim(), ignoreCase = true) }
                ?.also { unpaired -= it }
        }
        ProfilePairing(profile.id, profile.name, pairedWith)
    }
}

internal class LocalProfile(val id: Long, val uuid: String, val name: String)

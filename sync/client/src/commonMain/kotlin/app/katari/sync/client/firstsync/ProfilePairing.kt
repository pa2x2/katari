package app.katari.sync.client.firstsync

import app.katari.sync.client.kind.profile.ProfileKind

/** A profile the account already has, from its record. */
data class AccountProfile(val uuid: String, val name: String)

/**
 * One of this device's profiles, and the account profile proposed as the same person's; null to add it as new.
 * [sameProfile] when it already is that account profile (it carries its identity, as the default profile and every
 * profile shared before the server started over do), so it pairs whatever the person picks.
 */
data class ProfilePairing(
    val profileId: Long,
    val name: String,
    val pairedWith: AccountProfile?,
    val sameProfile: Boolean,
)

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
 * Pairs [local] profiles with [account] ones, each account profile with one device profile at most. A profile that
 * already carries an account profile's identity is that profile; the rest pair by name, ignoring case and surrounding
 * spaces, among the account profiles no device profile is yet, so two profiles of one name pair with two of the
 * account's. The default profile only ever pairs with the account's default, whatever either is called.
 */
internal fun proposePairings(local: List<LocalProfile>, account: List<AccountProfile>): List<ProfilePairing> {
    val byIdentity = account.associateBy { it.uuid }
    val localIdentities = local.mapTo(HashSet()) { it.uuid }
    val unpaired = account
        .filterNot { it.uuid in localIdentities || it.uuid == ProfileKind.DEFAULT_UUID }
        .toMutableList()
    return local.map { profile ->
        val same = byIdentity[profile.uuid]
        val pairedWith = when {
            same != null -> same
            profile.uuid == ProfileKind.DEFAULT_UUID -> null
            else -> unpaired.firstOrNull { it.name.trim().equals(profile.name.trim(), ignoreCase = true) }
                ?.also { unpaired -= it }
        }
        ProfilePairing(profile.id, profile.name, pairedWith, sameProfile = same != null)
    }
}

internal class LocalProfile(val id: Long, val uuid: String, val name: String)

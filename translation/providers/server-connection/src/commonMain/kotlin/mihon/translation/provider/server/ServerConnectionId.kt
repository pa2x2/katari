package mihon.translation.provider.server

/**
 * Where one provider's server connection is saved: [preferencesName] names the file of its settings and [apiKeyAlias]
 * its API key in the [SecretStore], which shows the key to the user as [apiKeyLabel] where it lists what it keeps. The
 * names stay those of connections saved by earlier versions.
 */
class ServerConnectionId(
    val preferencesName: String,
    val apiKeyAlias: String,
    val apiKeyLabel: String,
)

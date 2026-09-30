package mihon.translation.provider.deepl

import android.content.Context
import mihon.translation.provider.server.ServerConnectionConfiguration

internal fun deepLServerConfiguration(context: Context) = ServerConnectionConfiguration(
    context = context,
    preferencesName = "translation.deepl-server",
    apiKeyAlias = "katari.translation.deepl.api-key",
)

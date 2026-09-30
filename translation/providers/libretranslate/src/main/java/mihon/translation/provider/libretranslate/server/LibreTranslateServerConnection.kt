package mihon.translation.provider.libretranslate.server

import android.content.Context
import mihon.translation.provider.server.ServerConnectionConfiguration

/** Where the LibreTranslate server connection is stored; the names keep connections saved by earlier versions. */
internal fun libreTranslateServerConfiguration(context: Context) = ServerConnectionConfiguration(
    context = context,
    preferencesName = "translation.libretranslate-server",
    apiKeyAlias = "katari.translation.libretranslate.api-key",
)

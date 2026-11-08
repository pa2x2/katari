package mihon.translation.provider.server.setup

import android.app.Activity
import kotlin.reflect.KClass

/** The activity Android shows [screen] alone in; its manifest entry is the provider's. */
class ProviderSetupShell(
    val screen: KClass<out ProviderSetupScreen>,
    val activity: Class<out Activity>,
)

package mihon.translation.provider.server.setup

import android.content.Context
import android.content.Intent

/** Opens a provider's setup screen in its [ProviderSetupShell], as a task of its own over whatever asked for it. */
internal class ActivityServerSetupDestination(
    private val context: Context,
    shells: Set<ProviderSetupShell>,
) : ServerSetupDestination {
    private val activities = shells.associate { it.screen to it.activity }

    override suspend fun open(screen: ProviderSetupScreen): Boolean {
        val activity = activities[screen::class] ?: return false
        return runCatching {
            context.startActivity(Intent(context, activity).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.isSuccess
    }
}

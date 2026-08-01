package mihon.entry.interactions.runtime

import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import mihon.entry.interactions.reader.settings.ReaderBasePreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** Applies the app's secure-screen policy to a reader or player owned by an entry interaction. */
fun AppCompatActivity.registerEntryInteractionSecureScreen() {
    val readerPreferences = Injekt.get<ReaderBasePreferences>()
    val securityPreferences = Injekt.get<SecurityPreferences>()
    combine(
        securityPreferences.secureScreen.changes(),
        readerPreferences.incognitoMode.changes(),
    ) { secureScreen, incognitoMode ->
        secureScreen == SecurityPreferences.SecureScreenMode.ALWAYS ||
            (secureScreen == SecurityPreferences.SecureScreenMode.INCOGNITO && incognitoMode)
    }
        .onEach { enabled ->
            if (enabled) {
                window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
        .launchIn(lifecycleScope)
}

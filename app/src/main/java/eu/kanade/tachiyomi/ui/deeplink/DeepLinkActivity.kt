package eu.kanade.tachiyomi.ui.deeplink

import android.app.Activity
import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import eu.kanade.tachiyomi.ui.main.MainActivity

class DeepLinkActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createMainActivityIntent(intent)?.let(::startActivity)
        finish()
    }

    private fun createMainActivityIntent(source: Intent): Intent? {
        val action = source.action?.takeIf(SUPPORTED_ACTIONS::contains) ?: return null
        return Intent(applicationContext, MainActivity::class.java).apply {
            this.action = action
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)

            when (action) {
                Intent.ACTION_SEARCH,
                GOOGLE_SEARCH_ACTION,
                -> source.getStringExtra(SearchManager.QUERY)?.let { putExtra(SearchManager.QUERY, it) }
                Intent.ACTION_SEND -> source.getStringExtra(Intent.EXTRA_TEXT)?.let { putExtra(Intent.EXTRA_TEXT, it) }
                MainActivity.INTENT_SEARCH -> {
                    source.getStringExtra(MainActivity.INTENT_SEARCH_QUERY)
                        ?.let { putExtra(MainActivity.INTENT_SEARCH_QUERY, it) }
                    source.getStringExtra(MainActivity.INTENT_SEARCH_FILTER)
                        ?.let { putExtra(MainActivity.INTENT_SEARCH_FILTER, it) }
                }
            }
        }
    }

    private companion object {
        const val GOOGLE_SEARCH_ACTION = "com.google.android.gms.actions.SEARCH_ACTION"

        val SUPPORTED_ACTIONS = setOf(
            Intent.ACTION_APPLICATION_PREFERENCES,
            Intent.ACTION_SEARCH,
            Intent.ACTION_SEND,
            GOOGLE_SEARCH_ACTION,
            MainActivity.INTENT_SEARCH,
        )
    }
}

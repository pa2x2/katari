package eu.kanade.tachiyomi.source.javascript

import app.cash.quickjs.QuickJs
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.network.JavaScriptEngine

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class QuickJsJavaScriptEngine : JavaScriptEngine() {

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T> evaluate(script: String): T = QuickJs.create().use { it.evaluateSuspending(script) as T }
}

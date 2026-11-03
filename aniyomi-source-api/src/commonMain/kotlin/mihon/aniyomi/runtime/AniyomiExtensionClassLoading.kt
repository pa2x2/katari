package mihon.aniyomi.runtime

/**
 * Packages an Aniyomi extension's class loader must take from the app before its own APK.
 *
 * Extensions load their own classes first, so the libraries they bundle (Rhino, Apache Commons, NanoHTTPD) stay
 * theirs. Types that cross between the extension and Katari must be one class on both sides, though: an APK that
 * bundles the Kotlin runtime would otherwise get its own `Continuation` and `Function1`, and the first suspend call
 * from Katari into the source would fail. Most salmanbappi extensions bundle it.
 */
object AniyomiExtensionClassLoading {

    val parentFirstPackagePrefixes: List<String> = listOf(
        "kotlin.",
        "kotlinx.coroutines.",
        "kotlinx.serialization.",
        "okhttp3.",
        "okio.",
        "org.jsoup.",
        "uy.kohesive.injekt.",
        "androidx.preference.",
        "eu.kanade.tachiyomi.animesource.",
        "eu.kanade.tachiyomi.network.",
        "eu.kanade.tachiyomi.torrentutils.",
        "eu.kanade.tachiyomi.util.",
        "eu.kanade.tachiyomi.AppInfo",
    )
}

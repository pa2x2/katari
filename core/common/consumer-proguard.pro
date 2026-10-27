# The Entry SDK publishes this module, and extensions link against these classes by exact signature. R8 must neither
# remove them nor rewrite them: it turns unkept Kotlin objects into static methods and narrows return types, which
# breaks every extension call. verifyExtensionRuntimeAbi fails when a class the SDK covers isn't kept here.
-keep class eu.kanade.tachiyomi.network.** { *; }
-keep class tachiyomi.core.common.util.lang.** { public protected *; }

# The app has its own classes in these packages, so the module's classes are named one by one.
-keep class eu.kanade.tachiyomi.util.lang.Hash { public protected *; }
-keep class eu.kanade.tachiyomi.util.lang.StringExtensionsKt { public protected *; }
-keep class eu.kanade.tachiyomi.util.storage.DiskUtil { public protected *; }
-keep class eu.kanade.tachiyomi.util.system.DensityExtensionsKt { public protected *; }
-keep class eu.kanade.tachiyomi.util.system.DeviceUtil { public protected *; }
-keep class eu.kanade.tachiyomi.util.system.GLUtil { public protected *; }
-keep class eu.kanade.tachiyomi.util.system.GLUtilKt { public protected *; }
-keep class eu.kanade.tachiyomi.util.system.ToastExtensionsKt { public protected *; }
-keep class eu.kanade.tachiyomi.util.system.WebViewUtil { public protected *; }
-keep class eu.kanade.tachiyomi.util.system.WebViewUtilKt { public protected *; }

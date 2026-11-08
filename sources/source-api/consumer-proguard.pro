-keep class eu.kanade.tachiyomi.source.* { public protected *; }
-keep class eu.kanade.tachiyomi.source.model.** { public protected *; }
-keep class eu.kanade.tachiyomi.source.online.** { public protected *; }
-keep class eu.kanade.tachiyomi.source.** extends eu.kanade.tachiyomi.source.Source { public protected *; }

-keep,allowoptimization class eu.kanade.tachiyomi.util.JsoupExtensionsKt { public protected *; }
-keep,allowoptimization class eu.kanade.tachiyomi.AppInfo { public protected *; }

# AppInfo reports the app version to extensions by reading these fields reflectively, so nothing else keeps them
-keep class eu.kanade.tachiyomi.BuildConfig {
    public static final int VERSION_CODE;
    public static final java.lang.String VERSION_NAME;
}

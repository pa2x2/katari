-keep class eu.kanade.tachiyomi.source.entry.** { public protected *; }
-keep class eu.kanade.tachiyomi.source.entry.** implements eu.kanade.tachiyomi.source.entry.UnifiedSource { public protected *; }
-keep class eu.kanade.tachiyomi.source.entry.** implements eu.kanade.tachiyomi.source.entry.EntrySourceFactory { public protected *; }

-keep,includedescriptorclasses class eu.kanade.tachiyomi.source.entry.**$$serializer { *; }
-keepclassmembers class eu.kanade.tachiyomi.source.entry.** {
    *** Companion;
}
-keepclasseswithmembers class eu.kanade.tachiyomi.source.entry.** {
    kotlinx.serialization.KSerializer serializer(...);
}

package mihon.core.metro

import dev.zacsweers.metro.Qualifier

/** Qualifies the [tachiyomi.core.common.preference.PreferenceStore] whose values every profile shares. */
@Qualifier
@Target(
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.PROPERTY_GETTER,
)
@Retention(AnnotationRetention.RUNTIME)
annotation class BasePreferenceStore

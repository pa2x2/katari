package app.katari.sync.client.setting.scope

/**
 * The preference keys of one part of the app, each marked [PreferenceScope.PERSON], [PreferenceScope.DEVICE_BY_DEFAULT]
 * or [PreferenceScope.DEVICE] with why it only works on the device, and the Settings [area] the synced ones belong to
 * ([Builder.area] names another for some of them).
 *
 * Every key a preference store is asked for anywhere in the app is marked in exactly one table, which the build
 * enforces: `checkPreferenceScopes` reads the tables from their source and fails on a key no table marks, on a key two
 * tables mark, and on a mark no key uses. So the keys here are string literals, never constants or expressions. A
 * prefix marks a family of keys whose suffix only exists at run time, such as a source key or a settings surface.
 *
 * Keys under [tachiyomi.core.common.preference.Preference]'s app-state or private prefix are the device's by
 * definition and are not listed.
 */
class PreferenceScopeTable(area: SettingsArea? = null, declare: Builder.() -> Unit) {
    internal val keys: Map<String, PreferenceMark>
    internal val prefixes: Map<String, PreferenceMark>

    init {
        val builder = Builder(area).apply(declare)
        keys = builder.keys
        prefixes = builder.prefixes
    }

    class Builder internal constructor(private var area: SettingsArea?) {
        internal val keys = LinkedHashMap<String, PreferenceMark>()
        internal val prefixes = LinkedHashMap<String, PreferenceMark>()

        /** Marks the keys [declare] marks as belonging to [area] rather than the table's. */
        fun area(area: SettingsArea, declare: Builder.() -> Unit) {
            val table = this.area
            this.area = area
            declare()
            this.area = table
        }

        fun person(vararg keys: String) = mark(this.keys, keys, synced(PreferenceScope.PERSON))

        fun deviceByDefault(vararg keys: String) = mark(this.keys, keys, synced(PreferenceScope.DEVICE_BY_DEFAULT))

        fun device(bound: DeviceBound, vararg keys: String) = mark(this.keys, keys, bound(bound))

        fun personPrefix(vararg prefixes: String) = mark(this.prefixes, prefixes, synced(PreferenceScope.PERSON))

        fun deviceByDefaultPrefix(vararg prefixes: String) =
            mark(this.prefixes, prefixes, synced(PreferenceScope.DEVICE_BY_DEFAULT))

        fun devicePrefix(bound: DeviceBound, vararg prefixes: String) = mark(this.prefixes, prefixes, bound(bound))

        private fun synced(scope: PreferenceScope): PreferenceMark {
            val area = requireNotNull(area) { "Keys that can sync belong to a Settings area" }
            return PreferenceMark(scope, area, bound = null)
        }

        private fun bound(bound: DeviceBound) = PreferenceMark(PreferenceScope.DEVICE, area = null, bound)

        private fun mark(into: MutableMap<String, PreferenceMark>, keys: Array<out String>, mark: PreferenceMark) {
            keys.forEach { key -> require(into.put(key, mark) == null) { "$key is marked twice" } }
        }
    }
}

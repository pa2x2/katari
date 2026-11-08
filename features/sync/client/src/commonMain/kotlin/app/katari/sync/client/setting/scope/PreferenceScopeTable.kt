package app.katari.sync.client.setting.scope

/**
 * The preference keys of one area of the app, each marked [PreferenceScope.PERSON] or [PreferenceScope.DEVICE].
 *
 * Every key a preference store is asked for anywhere in the app is marked in exactly one table, which the build
 * enforces: `checkPreferenceScopes` reads the tables from their source and fails on a key no table marks, on a key two
 * tables mark, and on a mark no key uses. So the keys here are string literals, never constants or expressions. A
 * prefix marks a family of keys whose suffix only exists at run time, such as a source key or a settings surface.
 *
 * Keys under [tachiyomi.core.common.preference.Preference]'s app-state or private prefix are the device's by
 * definition and are not listed.
 */
class PreferenceScopeTable(declare: Builder.() -> Unit) {
    internal val keys: Map<String, PreferenceScope>
    internal val prefixes: Map<String, PreferenceScope>

    init {
        val builder = Builder().apply(declare)
        keys = builder.keys
        prefixes = builder.prefixes
    }

    class Builder internal constructor() {
        internal val keys = LinkedHashMap<String, PreferenceScope>()
        internal val prefixes = LinkedHashMap<String, PreferenceScope>()

        fun person(vararg keys: String) = mark(this.keys, keys, PreferenceScope.PERSON)

        fun device(vararg keys: String) = mark(this.keys, keys, PreferenceScope.DEVICE)

        fun personPrefix(vararg prefixes: String) = mark(this.prefixes, prefixes, PreferenceScope.PERSON)

        fun devicePrefix(vararg prefixes: String) = mark(this.prefixes, prefixes, PreferenceScope.DEVICE)

        private fun mark(into: MutableMap<String, PreferenceScope>, keys: Array<out String>, scope: PreferenceScope) {
            keys.forEach { key -> require(into.put(key, scope) == null) { "$key is marked twice" } }
        }
    }
}

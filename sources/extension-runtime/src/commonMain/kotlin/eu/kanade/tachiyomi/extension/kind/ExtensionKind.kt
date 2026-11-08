package eu.kanade.tachiyomi.extension.kind

import tachiyomi.domain.source.identity.SourceIdScheme
import tachiyomi.domain.source.identity.SourceProviderKind

/**
 * One extension ecosystem's rules. The loader does what every extension needs, whichever ecosystem built it: finding the
 * installed ones, trust, content warnings and reusing an unchanged extension. A kind adds what its ecosystem decides:
 * which library versions it runs and how its extensions become sources. Apks are [PackageExtensionKind]s; extensions
 * the app downloads and runs itself are [eu.kanade.tachiyomi.extension.file.FileExtensionKind]s.
 */
internal interface ExtensionKind {

    /** The ID scheme of the IDs its sources were given by their authors. */
    val sourceIdScheme: SourceIdScheme

    val providerKind: SourceProviderKind

    /** The library versions it runs, as `app/extension-compatibility.json` lists them. */
    val family: ExtensionFamily

    /** Whether it runs extensions built against [libVersionName], as a manifest or a store listing declares it. */
    fun runsLib(libVersionName: String): Boolean

    /**
     * Whether [libVersionName] is a version of its library, whether or not it runs that version. Kinds sharing a
     * feature tell apart by it which one a package they all refuse was built for.
     */
    fun isBuiltFor(libVersionName: String): Boolean

    /** Whether [libVersionName], a version it doesn't run, is newer than all it does; `null` when it can't be read. */
    fun isNewerThanRun(libVersionName: String): Boolean?
}

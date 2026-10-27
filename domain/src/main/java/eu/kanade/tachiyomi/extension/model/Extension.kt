package eu.kanade.tachiyomi.extension.model

import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import mihon.domain.extension.model.ContentWarning
import mihon.domain.extension.model.ExtensionStore
import tachiyomi.domain.source.identity.SourceIdScheme
import tachiyomi.domain.source.identity.SourceProviderKind
import tachiyomi.domain.source.model.StubSource

sealed interface Extension {

    val name: String

    /** An apk's package name; for a file, its kind's directory and its own ID, as `kind:id`. */
    val pkgName: String
    val versionName: String

    /** An apk's version code; 0 for a file, whose versions its kind orders by [versionName]. */
    val versionCode: Long
    val libVersion: Double?
    val lang: String?
    val contentWarning: ContentWarning

    /**
     * An extension that is on the device, whether or not it ended up being loaded.
     */
    sealed interface Installed : Extension {
        val form: InstalledForm

        val hasUpdate: Boolean

        val store: ExtensionStore?
    }

    /**
     * An extension that isn't on the device yet, as listed by an [ExtensionStore].
     */
    data class Available(
        override val name: String,
        override val pkgName: String,
        override val versionName: String,
        override val versionCode: Long,
        override val libVersion: Double,
        override val lang: String,
        override val contentWarning: ContentWarning,
        val sources: List<Source>,
        val download: ListedDownload,
        val iconUrl: String,
        val store: ExtensionStore,
        val libVersionName: String,
        /** The ecosystem whose rules minted the IDs of its [sources], as its store's index says. */
        val sourceIdScheme: SourceIdScheme,
        /** Finds its episodes as torrents, which the app can't play, so it's listed but can't be installed. */
        val isTorrent: Boolean,
    ) : Extension {

        data class Source(
            val id: Long,
            val lang: String,
            val name: String,
            val baseUrl: String,
            val supportedEntryTypes: Set<EntryType>? = null,
        ) {
            fun toStubSource(): StubSource {
                return StubSource(
                    id = this.id,
                    lang = this.lang,
                    name = this.name,
                )
            }
        }
    }

    /**
     * An installed extension whose sources are registered and usable.
     */
    data class Loaded(
        override val name: String,
        override val pkgName: String,
        override val versionName: String,
        override val versionCode: Long,
        override val libVersion: Double,
        override val lang: String,
        override val contentWarning: ContentWarning,
        override val form: InstalledForm,
        val pkgFactory: String?,
        val sources: List<UnifiedSource>,
        /** Declared by the loader that loaded it, for the IDs its sources report. */
        val sourceIdScheme: SourceIdScheme,
        val providerKind: SourceProviderKind,
        /** Sources it also has that another extension provides, so they're registered from that one instead. */
        val sourceConflicts: List<SourceConflict> = emptyList(),
        override val hasUpdate: Boolean = false,
        val isObsolete: Boolean = false,
        override val store: ExtensionStore? = null,
    ) : Installed

    /**
     * An installed extension that was never loaded, so it provides no sources. [lang] is derived
     * from the sources and [libVersion] from metadata, so neither is always known here.
     */
    data class NotLoaded(
        override val name: String,
        override val pkgName: String,
        override val versionName: String,
        override val versionCode: Long,
        override val form: InstalledForm,
        override val contentWarning: ContentWarning,
        override val libVersion: Double? = null,
        override val lang: String? = null,
        override val hasUpdate: Boolean = false,
        override val store: ExtensionStore? = null,
        val reason: Reason,
    ) : Installed {

        sealed interface Reason {
            /**
             * Its signature, or for a file its code's SHA-256, isn't trusted yet. Resolvable by the user accepting it.
             */
            data class Untrusted(val signatureHash: String) : Reason

            /** Its [contentWarning] isn't one the user chose to load. */
            data object Filtered : Reason

            /** No signature to check against at all. */
            data object Unsigned : Reason

            /**
             * Built against [libVersionName] of an extension library this app version can't run. [providerKind] is the
             * loader whose library it is.
             */
            data class UnsupportedLibVersion(val libVersionName: String, val providerKind: SourceProviderKind) : Reason

            /** Finds its episodes as torrents, which the app can't play. */
            data object Torrent : Reason

            /** Required package metadata is missing. */
            data object Malformed : Reason

            /**
             * Its private apk is one Android couldn't parse, or its file's manifest couldn't be read, so nothing is known
             * about it but the name it's stored under.
             */
            data object Unreadable : Reason

            /** Its file no longer is the one that was installed, so it isn't run. */
            data object Damaged : Reason

            /** Threw while its classes or sources were being instantiated. */
            data class Failed(val message: String, val stackTrace: String) : Reason

            /** Loaded, but other extensions provide every one of its sources. */
            data class SourceConflicts(val conflicts: List<SourceConflict>) : Reason
        }
    }
}

/** What an installed extension is on the device. */
sealed interface InstalledForm {

    /** An apk: installed for every app ([isShared]) or as a private copy in this app's files. */
    data class Package(
        val isShared: Boolean,
        /** SHA-256 digests of the certificates it's signed with. */
        val signatures: List<String>,
    ) : InstalledForm

    /** A file this app downloaded and runs itself, from the store at [storeIndexUrl], whose code hashes to [sha256]. */
    data class File(
        val sha256: String,
        val storeIndexUrl: String,
    ) : InstalledForm
}

/**
 * The package name of a file extension whose own ID among [scheme]'s is [id], as its listings and its install name it.
 * A scheme's key is lower-case letters, so this can never be an Android package name.
 */
fun fileExtensionPkgName(scheme: SourceIdScheme, id: String): String = "${scheme.key}:$id"

/** What a store's listing downloads to install. */
sealed interface ListedDownload {

    data class Apk(val url: String) : ListedDownload

    /** A file the app runs itself, with the [companions] its kind keeps beside it, by file name. */
    data class File(
        val url: String,
        val companions: Map<String, String> = emptyMap(),
    ) : ListedDownload
}

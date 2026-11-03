package eu.kanade.tachiyomi.data.backup.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/**
 * An installed file extension, by what installs it again: the store it came from and its package name there. Its file
 * isn't kept; restoring installs the store's current version.
 */
@Serializable
class BackupFileExtension(
    @ProtoNumber(1) var pkgName: String,
    @ProtoNumber(2) var name: String,
    @ProtoNumber(3) var storeIndexUrl: String,
    @ProtoNumber(4) var versionName: String,
)

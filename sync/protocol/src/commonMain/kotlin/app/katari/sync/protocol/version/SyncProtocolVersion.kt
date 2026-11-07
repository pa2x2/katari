package app.katari.sync.protocol.version

/**
 * The protocol is versioned as a whole by one integer. A server accepts a range of versions ([ProtocolVersionRange])
 * and refuses the others with [app.katari.sync.protocol.error.SyncErrorCode.PROTOCOL_VERSION_UNSUPPORTED].
 *
 * Messages evolve the protobuf way without a new version: a field added later takes an unused number and a Kotlin
 * default, so older peers skip it and newer peers read its absence as the default. Field numbers are never reused or
 * renumbered. The version goes up only when an older peer would get a message wrong: a field changes meaning, a new
 * field must not be ignored, or an enum gains an entry (kotlinx protobuf refuses enum numbers it does not know, so a
 * peer sends only the entries the other side's version has).
 *
 * Record payloads are not covered: each kind versions its own format
 * ([app.katari.sync.protocol.record.RecordEnvelope.kindVersion]), and the server never needs to understand it.
 */
object SyncProtocolVersion {
    const val CURRENT: Int = 1
}

package app.katari.core.common.platform

/**
 * Marks a contract each platform answers with its own implementation: an operating-system or platform service, a
 * viewer or player until the platform has one, or a fact of the platform's own history. Everything else a platform
 * binds is the shared implementation, which answers from these.
 *
 * `checkPlatformFacilities` holds both sides to it: a platform-only class may implement a shared interface only if
 * it is a facility, and no shared class implements, nor shared binding binds, a facility. So a graph that leaves out
 * a platform's implementation fails to compile rather than falling back to a shared one. A platform without the
 * service binds a stand-in that reports it as not offered through the contract's own members.
 *
 * Kept to the sources, so the extension SDK classes it marks compile as they did.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class PlatformFacility

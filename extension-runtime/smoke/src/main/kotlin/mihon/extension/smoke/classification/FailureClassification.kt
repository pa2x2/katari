package mihon.extension.smoke.classification

import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.InstallStep
import eu.kanade.tachiyomi.network.HttpException
import mihon.extension.runtime.jvm.conversion.DexConversionException
import mihon.extension.runtime.jvm.conversion.unconvertedMethodOf
import mihon.extension.runtime.jvm.diagnostics.PlatformGaps
import mihon.extension.smoke.results.Outcome
import mihon.extension.smoke.results.OutcomeKind
import java.io.InterruptedIOException

/**
 * Names why an install, a load or a source call failed, from what the host reports: the platform gaps it recognises,
 * its conversion and signature errors, and otherwise the extension's own error with its type and the site involved.
 */
internal class FailureClassification(private val cloudflare: CloudflareProbe) {

    suspend fun ofCall(error: Throwable, siteUrl: String?): Outcome {
        PlatformGaps.missingMember(error)?.let { return platformGap(it, error) }
        unconvertedMethodOf(error)?.let { return Outcome(OutcomeKind.CONVERSION_FAILED, error.rootText, member = it) }
        val causes = error.causes()
        causes.filterIsInstance<LinkageError>().firstOrNull()?.let { linkage ->
            return Outcome(
                OutcomeKind.PLATFORM_GAP,
                linkage.message.orEmpty(),
                member = linkedMember(linkage.message.orEmpty()),
                errorType = linkage::class.java.name,
            )
        }
        if (causes.any { it.message.orEmpty().contains(UNTRUSTED_CERTIFICATE) }) {
            // The JDK's trust store, not Android's: a root Android ships may be missing from it
            return Outcome(OutcomeKind.PLATFORM_GAP, error.rootText, member = TRUST_STORE_GAP, url = siteUrl)
        }
        val root = causes.last()
        val errorType = causes.filterIsInstance<HttpException>().firstOrNull()?.let { "HTTP ${it.code}" }
            ?: root::class.java.name
        // OkHttp's call and socket timeouts: the site is too slow, as when the run's own timeout ends a call
        if (root is InterruptedIOException && root.message.orEmpty().contains("timeout", ignoreCase = true)) {
            return Outcome(OutcomeKind.TIMEOUT, error.rootText, errorType = errorType, url = siteUrl)
        }
        // Whatever the extension makes of a challenge (an HTTP error, a script's own message, an unreadable page), the
        // site asked for a web view when it challenges the run's own request too
        if (siteUrl != null && cloudflare.isChallenged(siteUrl)) {
            return Outcome(
                OutcomeKind.NEEDS_WEB_VIEW,
                "Cloudflare challenge: ${error.rootText}",
                errorType = errorType,
                url = siteUrl,
            )
        }
        return Outcome(OutcomeKind.EXTENSION_ERROR, error.rootText, errorType = errorType, url = siteUrl)
    }

    fun ofInstall(error: InstallStep.Error): Outcome {
        val trace = error.stackTrace.orEmpty()
        return when {
            DEX_CONVERSION_EXCEPTION in trace -> Outcome(OutcomeKind.CONVERSION_FAILED, error.message)
            // The listing's check that its store signed the apk, which also fails for an apk whose signatures don't
            // verify, since such an apk has no signers
            STORE_SIGNATURE_CHECK in trace -> Outcome(OutcomeKind.VERIFICATION_FAILED, error.message)
            else -> Outcome(
                OutcomeKind.INSTALL_FAILED,
                error.message,
                errorType = trace.lineSequence().firstOrNull()?.substringBefore(':'),
            )
        }
    }

    fun ofNotLoaded(reason: Extension.NotLoaded.Reason): Outcome = when (reason) {
        is Extension.NotLoaded.Reason.Failed -> {
            val type = reason.message.substringBefore(':')
            val missing = reason.missingPlatformMember
            when {
                missing != null -> platformGap(missing, reason.message)
                DEX_CONVERSION_EXCEPTION in reason.stackTrace -> Outcome(OutcomeKind.CONVERSION_FAILED, reason.message)
                type in LINKAGE_ERRORS -> Outcome(
                    OutcomeKind.PLATFORM_GAP,
                    reason.message,
                    member = linkedMember(reason.message.substringAfter(':').trim()),
                    errorType = type,
                )
                else -> Outcome(OutcomeKind.LOAD_FAILED, reason.message, errorType = type)
            }
        }
        is Extension.NotLoaded.Reason.Untrusted ->
            Outcome(OutcomeKind.VERIFICATION_FAILED, "Signed with a key no store vouches for: ${reason.signatureHash}")
        Extension.NotLoaded.Reason.Unsigned -> Outcome(OutcomeKind.VERIFICATION_FAILED, "Not signed")
        is Extension.NotLoaded.Reason.UnsupportedLibVersion ->
            Outcome(OutcomeKind.NOT_INSTALLABLE, "Library ${reason.libVersionName}")
        Extension.NotLoaded.Reason.Torrent -> Outcome(OutcomeKind.NOT_INSTALLABLE, "Torrent extension")
        Extension.NotLoaded.Reason.Filtered -> Outcome(OutcomeKind.LOAD_FAILED, "Left out by the content warnings")
        Extension.NotLoaded.Reason.Malformed -> Outcome(OutcomeKind.LOAD_FAILED, "Required package metadata missing")
        Extension.NotLoaded.Reason.Unreadable -> Outcome(OutcomeKind.LOAD_FAILED, "The apk or file can't be read")
        Extension.NotLoaded.Reason.Damaged -> Outcome(OutcomeKind.LOAD_FAILED, "The file isn't the one installed")
        is Extension.NotLoaded.Reason.SourceConflicts ->
            Outcome(OutcomeKind.LOAD_FAILED, "Every source is provided by another installed extension")
    }

    private fun platformGap(member: String, error: Throwable) = platformGap(member, error.rootText)

    private fun platformGap(member: String, detail: String): Outcome {
        val kind = if (member.startsWith(WEB_VIEW_PACKAGE)) OutcomeKind.NEEDS_WEB_VIEW else OutcomeKind.PLATFORM_GAP
        return Outcome(kind, detail, member = member)
    }

    private companion object {
        const val UNTRUSTED_CERTIFICATE = "PKIX path building failed"
        const val TRUST_STORE_GAP = "TLS certificate trust (PKIX path building failed)"
        const val WEB_VIEW_PACKAGE = "android.webkit."
        val DEX_CONVERSION_EXCEPTION = DexConversionException::class.java.name
        const val STORE_SIGNATURE_CHECK = "requireSignedByStore"
        val LINKAGE_ERRORS = setOf(
            "NoClassDefFoundError",
            "NoSuchMethodError",
            "NoSuchFieldError",
            "AbstractMethodError",
            "IncompatibleClassChangeError",
            "VerifyError",
            "UnsatisfiedLinkError",
        )
    }
}

private fun Throwable.causes(): List<Throwable> =
    generateSequence(this) { it.cause.takeIf { cause -> cause !== it } }.take(MAX_CAUSES).toList()

/** The deepest cause's type and message, which say what actually went wrong. */
private val Throwable.rootText: String
    get() {
        val root = causes().last()
        return listOfNotNull(root::class.simpleName, root.message).joinToString(": ")
    }

/**
 * The class or member a linkage error outside Android's packages names, such as a library the app has on Android and
 * not on desktop: the quoted member of a HotSpot message without its types, else the message as a class name.
 */
private fun linkedMember(message: String): String {
    val quoted = Regex("'([^']+)'").find(message)?.groupValues?.get(1)
    return quoted?.substringBefore('(')?.substringAfterLast(' ') ?: message.substringBefore(' ').replace('/', '.')
}

private const val MAX_CAUSES = 16

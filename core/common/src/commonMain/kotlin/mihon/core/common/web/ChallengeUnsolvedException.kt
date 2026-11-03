package mihon.core.common.web

/**
 * Why a challenged request got no clearance: the cause of the error the request then fails with, whose own message is
 * the one screens show for every challenge that wasn't solved. It tells a challenge that only a person can solve, in
 * the web view screen, apart from the app's own failures, as the extension compatibility harness counts them.
 */
class ChallengeUnsolvedException(val reason: ChallengeUnsolved) : Exception(reason.description)

enum class ChallengeUnsolved(val description: String) {
    /** The challenge asked for a person, which only the web view screen lets one be. */
    NEEDS_PERSON("The challenge needs a person to solve it"),

    /** The challenge ran and failed. */
    FAILED("The challenge failed"),

    /** The page got an error that isn't a challenge. */
    NOT_FOUND("The page found no challenge to solve"),

    /** The page ended before it got a clearance. */
    PAGE_GONE("The page ended before the challenge was solved"),

    /** The page got no clearance in the time a request waits. */
    TIMED_OUT("The challenge wasn't solved in time"),

    /** No page could open: the platform has none, or the request was made on the thread pages run on. */
    NO_PAGE("No page could open to solve the challenge"),

    /** The site challenged the request again with each clearance a page got. */
    REJECTED("The site rejected the clearance"),
}

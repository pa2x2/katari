package mihon.core.common.image.progressive

/** What a progressive decode has produced so far, for previews and readers that draw partial images. */
expect class ProgressiveImageState {
    /**
     * Whether it holds a whole animation that can play again from what was decoded, so a reader keeps showing it
     * rather than the finished file once that is ready.
     */
    val holdsReplayableAnimation: Boolean
}

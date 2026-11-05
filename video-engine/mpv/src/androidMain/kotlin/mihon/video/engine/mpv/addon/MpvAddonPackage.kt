package mihon.video.engine.mpv.addon

/** The mpv add-on as the libmpv release builds it: one package for every ABI and every Katari build type. */
object MpvAddonPackage {
    const val NAME = "app.katari.mpv"

    /** An integer: the JNI glue level of the add-on's `libplayer.so`, which must equal `MPVLib.GLUE_LEVEL`. */
    const val GLUE_META_DATA = "katari.libmpv.glue"
}

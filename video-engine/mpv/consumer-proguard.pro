# libplayer.so (the mpv add-on) finds MPVLib's callbacks and natives by name through JNI. The libmpv AAR carries this
# rule, but only the AAR's classes are taken (see build.gradle.kts), so it is restated here.
-keep class is.xyz.mpv.MPVLib { *; }

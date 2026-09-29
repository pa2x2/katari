# ML Kit instantiates its component registrars reflectively from manifest metadata. The keep rule of the
# firebase-components version it depends on keeps the registrar classes but, under R8 full mode, not their constructors,
# so builds without the newer Firebase that telemetry brings lose every ML Kit component.
-keep class * implements com.google.firebase.components.ComponentRegistrar { void <init>(); }

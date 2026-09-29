# ONNX Runtime's native library creates and reads its Java objects by name through JNI (tensor, exception and type
# constructors and factories that no Java code calls), and its AAR ships no keep rules for them.
-keep class ai.onnxruntime.** { *; }

# Kotlinx serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class com.zakir.vestra.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ONNX Runtime — JNI constructs NodeInfo/TensorInfo reflectively. R8 stripping
# caused Pixel try-on SIGABRT: NoSuchMethodError NodeInfo.<init>(String,ValueInfo).
# https://onnxruntime.ai/docs/build/android.html
-keep class ai.onnxruntime.** { *; }
-dontwarn ai.onnxruntime.**

# MediaPipe GenAI (local Gemma) — reflection + JNI
-keep class com.google.mediapipe.** { *; }
-dontwarn com.google.mediapipe.**


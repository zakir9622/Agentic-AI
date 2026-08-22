# ONNX Runtime JNI reflects into Java constructors (NodeInfo, TensorInfo, …).
# Without this, R8 strips them and release builds abort on OrtSession.getInputInfo
# with NoSuchMethodError → SIGABRT (Pixel try-on generate crash).
# See https://onnxruntime.ai/docs/build/android.html
-keep class ai.onnxruntime.** { *; }
-dontwarn ai.onnxruntime.**

package com.zakir.vestra.safety

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Fully offline face detection + blur for the privacy post-process (B7). ML Kit's face-detection
 * model ships bundled in the app (~6MB) — no network call, no Play Services dependency, works
 * with airplane mode on.
 */
object FaceBlurProcessor {

    /** Bounding boxes of every detected face, in [bitmap]'s own pixel coordinates. */
    suspend fun detectFaces(bitmap: Bitmap): List<Rect> {
        val detector = FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .build(),
        )
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            suspendCancellableCoroutine { cont ->
                detector.process(image)
                    .addOnSuccessListener { faces -> cont.resume(faces.map { it.boundingBox }) }
                    .addOnFailureListener { cont.resume(emptyList()) }
            }
        } finally {
            detector.close()
        }
    }

    /**
     * Detects faces in [bitmap] and returns a new bitmap with each detected region blurred.
     * Returns the original bitmap unchanged (same instance) when no faces are found.
     */
    suspend fun detectAndBlur(bitmap: Bitmap, blurRadius: Int = 25): Bitmap {
        val faces = detectFaces(bitmap)
        if (faces.isEmpty()) return bitmap
        val mutable = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        for (rect in faces) {
            BoxBlur.blurRegion(mutable, rect, blurRadius)
        }
        return mutable
    }
}

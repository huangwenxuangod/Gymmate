package com.gymmate.app.training

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import java.util.concurrent.atomic.AtomicBoolean

class CameraPoseAnalyzer(
    private val poseLandmarkerBridge: PoseLandmarkerBridge,
    private val onPoseDetected: (PoseDetectionState, PoseFrame?) -> Unit,
) : ImageAnalysis.Analyzer {

    private val isProcessing = AtomicBoolean(false)

    override fun analyze(imageProxy: ImageProxy) {
        if (!isProcessing.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }

        val bitmap = imageProxy.toBitmap()
        val rotatedBitmap = bitmap.rotate(imageProxy.imageInfo.rotationDegrees.toFloat())
        val mpImage = BitmapImageBuilder(rotatedBitmap).build()

        try {
            val poseLandmarker = poseLandmarkerBridge.poseLandmarker
            if (poseLandmarker == null) {
                onPoseDetected(PoseDetectionState(), null)
                return
            }

            val result = poseLandmarker.detect(mpImage)
            val firstPose = result.landmarks().firstOrNull().orEmpty()
            val points = firstPose.map {
                NormalizedPoint(
                    x = it.x(),
                    y = it.y(),
                    visibility = it.visibility().orElse(1f),
                )
            }
            onPoseDetected(
                PoseDetectionState(points = points, timestampMs = System.currentTimeMillis()),
                firstPose.toPoseFrame(),
            )
        } catch (_: Throwable) {
            onPoseDetected(PoseDetectionState(), null)
        } finally {
            mpImage.close()
            rotatedBitmap.recycle()
            if (rotatedBitmap != bitmap) {
                bitmap.recycle()
            }
            imageProxy.close()
            isProcessing.set(false)
        }
    }

    private fun Bitmap.rotate(rotationDegrees: Float): Bitmap {
        if (rotationDegrees == 0f) return this
        val matrix = Matrix().apply { postRotate(rotationDegrees) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }
}

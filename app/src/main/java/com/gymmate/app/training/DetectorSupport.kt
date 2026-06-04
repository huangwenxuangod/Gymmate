package com.gymmate.app.training

import kotlin.math.acos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

internal object DetectorSupport {
    fun averageConfidence(frame: PoseFrame, points: List<String>): Float {
        if (points.isEmpty()) return 0f
        return points.mapNotNull { frame.point(it)?.score }.average().toFloat()
    }

    fun averageAngle(
        leftA: PosePoint,
        leftB: PosePoint,
        leftC: PosePoint,
        rightA: PosePoint,
        rightB: PosePoint,
        rightC: PosePoint,
    ): Float = (angle(leftA, leftB, leftC) + angle(rightA, rightB, rightC)) / 2f

    fun angle(a: PosePoint, b: PosePoint, c: PosePoint): Float {
        val ab = Triple(a.x - b.x, a.y - b.y, a.z - b.z)
        val cb = Triple(c.x - b.x, c.y - b.y, c.z - b.z)
        val dot = ab.first * cb.first + ab.second * cb.second + ab.third * cb.third
        val abNorm = sqrt(ab.first.pow(2) + ab.second.pow(2) + ab.third.pow(2)).coerceAtLeast(1e-4f)
        val cbNorm = sqrt(cb.first.pow(2) + cb.second.pow(2) + cb.third.pow(2)).coerceAtLeast(1e-4f)
        val cosine = (dot / (abNorm * cbNorm)).coerceIn(-1f, 1f)
        return Math.toDegrees(acos(cosine).toDouble()).toFloat()
    }

    fun mid(left: PosePoint, right: PosePoint): PosePoint =
        PosePoint(
            x = (left.x + right.x) / 2f,
            y = (left.y + right.y) / 2f,
            z = (left.z + right.z) / 2f,
            score = (left.score + right.score) / 2f,
        )

    fun result(
        repCount: Int,
        visibility: Float,
        tags: List<String>,
        status: String,
        stage: String,
    ): RepetitionFeedback {
        val confidence = (visibility * 100).roundToInt().coerceIn(0, 99)
        val score = (95 - tags.size * 11 - if (confidence < 65) 12 else 0).coerceIn(52, 96)
        return RepetitionFeedback(
            repCount = repCount,
            score = score,
            errorTags = tags,
            status = status,
            stage = stage,
            confidence = confidence,
        )
    }

    fun waiting(repCount: Int, visibility: Float): RepetitionFeedback =
        emptyFeedback(
            repCount = repCount,
            status = "等待关键点稳定，尽量让肩、髋、膝、踝同时入镜",
            confidence = (visibility * 100).roundToInt(),
        )

    fun emptyFeedback(repCount: Int = 0, status: String, confidence: Int = 0) = RepetitionFeedback(
        repCount = repCount,
        score = 0,
        errorTags = emptyList(),
        status = status,
        stage = "准备",
        confidence = confidence,
    )
}

internal fun PoseFrame.point(name: String): PosePoint? = landmarks[name]

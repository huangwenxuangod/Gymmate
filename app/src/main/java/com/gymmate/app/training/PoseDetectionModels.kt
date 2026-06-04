package com.gymmate.app.training

data class PoseDetectionState(
    val points: List<NormalizedPoint> = emptyList(),
    val timestampMs: Long = 0L,
)

data class NormalizedPoint(
    val x: Float,
    val y: Float,
    val visibility: Float = 1f,
)


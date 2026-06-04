package com.gymmate.app.training

interface ExerciseDetector {
    fun reset()
    fun process(frame: PoseFrame, visibility: Float): RepetitionFeedback
}

internal enum class MotionPhase {
    START,
    DESCENDING,
    BOTTOM,
    ASCENDING,
    TOP,
    PULLING,
    PRESSING,
    RETURNING,
}

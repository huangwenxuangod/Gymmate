package com.gymmate.app.training

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

private val landmarkNames = listOf(
    "nose",
    "left_eye_inner",
    "left_eye",
    "left_eye_outer",
    "right_eye_inner",
    "right_eye",
    "right_eye_outer",
    "left_ear",
    "right_ear",
    "mouth_left",
    "mouth_right",
    "left_shoulder",
    "right_shoulder",
    "left_elbow",
    "right_elbow",
    "left_wrist",
    "right_wrist",
    "left_pinky",
    "right_pinky",
    "left_index",
    "right_index",
    "left_thumb",
    "right_thumb",
    "left_hip",
    "right_hip",
    "left_knee",
    "right_knee",
    "left_ankle",
    "right_ankle",
    "left_heel",
    "right_heel",
    "left_foot_index",
    "right_foot_index",
)

fun List<NormalizedLandmark>.toPoseFrame(): PoseFrame {
    val map = buildMap {
        landmarkNames.forEachIndexed { index, key ->
            val landmark = getOrNull(index) ?: return@forEachIndexed
            put(
                key,
                PosePoint(
                    x = landmark.x(),
                    y = landmark.y(),
                    z = landmark.z(),
                    score = landmark.visibility().orElse(1f),
                )
            )
        }
    }
    return PoseFrame(landmarks = map)
}

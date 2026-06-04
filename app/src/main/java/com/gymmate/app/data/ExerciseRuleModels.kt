package com.gymmate.app.data

data class ExerciseRuleConfig(
    val exercise_name: String,
    val camera_view: String,
    val required_landmarks: List<String>,
    val start_cue: String,
    val end_cue: String,
    val count_cue: String,
    val hard_fails: List<String>,
    val soft_warnings: List<String>,
)

data class ExerciseRuleListResponse(
    val items: List<ExerciseRuleConfig>,
)

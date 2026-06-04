package com.gymmate.app.data

data class Exercise(
    val id: Int,
    val name: String,
    val category: String,
    val targetMuscles: List<String>,
    val description: String,
    val standardSteps: List<String>,
    val commonErrors: List<String>,
    val correctionTips: List<String>,
    val mediaUrl: String,
    val difficultyLevel: String,
)

data class ExerciseListResponse(
    val items: List<Exercise>,
)

data class CategoryListResponse(
    val items: List<String>,
)

data class TrainingPlan(
    val id: Int,
    val name: String,
    val goalType: String,
    val level: String,
    val description: String,
    val schedule: List<String>,
)

data class TrainingPlanListResponse(
    val items: List<TrainingPlan>,
)

data class WorkoutHistoryItem(
    val id: Int,
    val exerciseName: String,
    val startedAt: String,
    val endedAt: String,
    val totalReps: Int,
    val avgScore: Int,
    val errorTags: List<String>,
    val summaryText: String,
)

data class WorkoutHistoryResponse(
    val items: List<WorkoutHistoryItem>,
)

data class TrendPoint(
    val date: String,
    val sessions: Int,
    val avgScore: Int,
)

data class WorkoutTrendResponse(
    val items: List<TrendPoint>,
)

data class WorkoutSessionCreateRequest(
    val exercise_id: Int,
    val started_at: String,
)

data class SessionCreatedResponse(
    val session_id: Int,
)

data class WorkoutSessionFinishRequest(
    val total_reps: Int,
    val avg_score: Int,
    val error_tags: List<String>,
    val summary_text: String,
    val ended_at: String,
)

data class FeedbackRequest(
    val exercise_name: String,
    val avg_score: Int,
    val error_tags: List<String>,
    val focus: String? = null,
)

data class FeedbackResponse(
    val summary: String,
    val topErrors: List<String>,
    val nextTimeFocus: String,
    val coachTip: String,
)

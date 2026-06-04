package com.gymmate.app.data

import com.gymmate.app.BuildConfig
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ApiRepository {
    private val service: ApiService by lazy {
        val gson = GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create()
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }

    suspend fun getExercises(query: String? = null, category: String? = null) =
        service.getExercises(query, category).items

    suspend fun getExerciseCategories() = service.getExerciseCategories().items
    suspend fun getExercise(id: Int) = service.getExercise(id)
    suspend fun getExerciseRules() = service.getExerciseRules().items
    suspend fun getExerciseRule(exerciseName: String) = service.getExerciseRule(exerciseName)
    suspend fun getExerciseRuleById(exerciseId: Int) = service.getExerciseRuleById(exerciseId)
    suspend fun getPlans() = service.getPlans().items
    suspend fun selectCurrentPlan(token: String, planId: Int) =
        service.selectCurrentPlan("Bearer $token", SelectPlanRequest(planId))
    suspend fun getCurrentPlan(token: String) = service.getCurrentPlan("Bearer $token")
    suspend fun getHistory(token: String) = service.getHistory("Bearer $token").items
    suspend fun getTrends(token: String, days: Int = 7) = service.getTrends("Bearer $token", days).items
    suspend fun sendCode(phone: String) = service.sendCode(SendOtpRequest(phone))
    suspend fun login(phone: String, code: String) = service.login(LoginRequest(phone, code))
    suspend fun getMe(token: String) = service.getMe("Bearer $token")
    suspend fun updateMe(
        token: String,
        nickname: String,
        gender: String?,
        heightCm: Int?,
        weightKg: Int?,
        fitnessGoal: String?,
        trainingLevel: String?,
    ) = service.updateMe(
        "Bearer $token",
        UpdateProfileRequest(
            nickname = nickname,
            gender = gender,
            heightCm = heightCm,
            weightKg = weightKg,
            fitnessGoal = fitnessGoal,
            trainingLevel = trainingLevel,
        ),
    )

    suspend fun createSession(token: String, exerciseId: Int, startedAt: String) =
        service.createSession("Bearer $token", WorkoutSessionCreateRequest(exerciseId, startedAt)).session_id

    suspend fun finishSession(
        sessionId: Int,
        token: String,
        totalReps: Int,
        avgScore: Int,
        errorTags: List<String>,
        summaryText: String,
        endedAt: String,
    ) = service.finishSession(
        sessionId,
        "Bearer $token",
        WorkoutSessionFinishRequest(
            total_reps = totalReps,
            avg_score = avgScore,
            error_tags = errorTags,
            summary_text = summaryText,
            ended_at = endedAt,
        ),
    )

    suspend fun getFeedback(
        exerciseName: String,
        avgScore: Int,
        errorTags: List<String>,
        focus: String? = null,
    ) = service.getFeedback(
        FeedbackRequest(
            exercise_name = exerciseName,
            avg_score = avgScore,
            error_tags = errorTags,
            focus = focus,
        ),
    )
}

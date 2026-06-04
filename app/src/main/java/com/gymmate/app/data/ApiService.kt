package com.gymmate.app.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @POST("auth/send-code")
    suspend fun sendCode(@Body body: SendOtpRequest): SendOtpResponse

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @GET("me")
    suspend fun getMe(@retrofit2.http.Header("Authorization") authorization: String): UserProfile

    @retrofit2.http.PUT("me")
    suspend fun updateMe(
        @retrofit2.http.Header("Authorization") authorization: String,
        @Body body: UpdateProfileRequest,
    ): UserProfile

    @GET("exercises")
    suspend fun getExercises(
        @Query("q") query: String? = null,
        @Query("category") category: String? = null,
    ): ExerciseListResponse

    @GET("exercise-categories")
    suspend fun getExerciseCategories(): CategoryListResponse

    @GET("exercises/{id}")
    suspend fun getExercise(@Path("id") id: Int): Exercise

    @GET("exercise-rules")
    suspend fun getExerciseRules(): ExerciseRuleListResponse

    @GET("exercise-rules/{exerciseName}")
    suspend fun getExerciseRule(@Path("exerciseName") exerciseName: String): ExerciseRuleConfig

    @GET("exercise-rule-by-id/{exerciseId}")
    suspend fun getExerciseRuleById(@Path("exerciseId") exerciseId: Int): ExerciseRuleConfig

    @GET("plans")
    suspend fun getPlans(): TrainingPlanListResponse

    @POST("user/plans/select")
    suspend fun selectCurrentPlan(
        @retrofit2.http.Header("Authorization") authorization: String,
        @Body body: SelectPlanRequest,
    ): UserProfile

    @GET("user/plans/current")
    suspend fun getCurrentPlan(
        @retrofit2.http.Header("Authorization") authorization: String,
    ): TrainingPlan

    @GET("workout/history")
    suspend fun getHistory(
        @retrofit2.http.Header("Authorization") authorization: String,
    ): WorkoutHistoryResponse

    @GET("workout/trends")
    suspend fun getTrends(
        @retrofit2.http.Header("Authorization") authorization: String,
        @Query("days") days: Int = 7,
    ): WorkoutTrendResponse

    @POST("workout/sessions")
    suspend fun createSession(
        @retrofit2.http.Header("Authorization") authorization: String,
        @Body body: WorkoutSessionCreateRequest,
    ): SessionCreatedResponse

    @POST("workout/sessions/{id}/finish")
    suspend fun finishSession(
        @Path("id") id: Int,
        @retrofit2.http.Header("Authorization") authorization: String,
        @Body body: WorkoutSessionFinishRequest,
    )

    @POST("ai/feedback")
    suspend fun getFeedback(@Body body: FeedbackRequest): FeedbackResponse
}

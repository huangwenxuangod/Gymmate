package com.gymmate.app.data

data class SendOtpRequest(
    val phone: String,
)

data class SendOtpResponse(
    val success: Boolean,
    val devCode: String?,
)

data class LoginRequest(
    val phone: String,
    val code: String,
)

data class UserProfile(
    val id: Int,
    val phone: String,
    val nickname: String,
    val gender: String?,
    val heightCm: Int?,
    val weightKg: Int?,
    val fitnessGoal: String?,
    val trainingLevel: String?,
    val currentPlanId: Int?,
)

data class LoginResponse(
    val token: String,
    val user: UserProfile,
)

data class UpdateProfileRequest(
    val nickname: String,
    val gender: String?,
    val heightCm: Int?,
    val weightKg: Int?,
    val fitnessGoal: String?,
    val trainingLevel: String?,
)

data class SelectPlanRequest(
    val trainingPlanId: Int,
)

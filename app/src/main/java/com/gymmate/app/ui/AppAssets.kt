package com.gymmate.app.ui

import androidx.annotation.DrawableRes
import com.gymmate.app.R

object AppAssets {
    @DrawableRes
    val homeHero = R.drawable.asset_home_hero

    @DrawableRes
    val loginHero = R.drawable.asset_login_hero

    @DrawableRes
    val profileHero = R.drawable.asset_profile_setup_hero

    @DrawableRes
    val emptyHistory = R.drawable.asset_empty_history

    @DrawableRes
    val emptySearch = R.drawable.asset_empty_search

    @DrawableRes
    val emptyCameraPermission = R.drawable.asset_empty_camera_permission

    @DrawableRes
    fun planCover(goal: String): Int = when {
        goal.contains("减脂") -> R.drawable.asset_plan_fat_loss
        goal.contains("力量") -> R.drawable.asset_plan_strength
        else -> R.drawable.asset_plan_muscle_gain
    }

    @DrawableRes
    fun categoryCover(category: String): Int = when (category) {
        "下肢" -> R.drawable.asset_category_legs
        "肩部" -> R.drawable.asset_category_shoulders
        "背部" -> R.drawable.asset_category_back
        "胸部" -> R.drawable.asset_category_chest
        else -> R.drawable.asset_home_hero
    }

    @DrawableRes
    fun exerciseCover(name: String): Int? = when (name) {
        "杠铃深蹲" -> R.drawable.asset_exercise_barbell_squat
        "哑铃推肩" -> R.drawable.asset_exercise_dumbbell_shoulder_press
        "哑铃侧平举" -> R.drawable.asset_exercise_dumbbell_lateral_raise
        "高位下拉" -> R.drawable.asset_exercise_lat_pulldown
        "坐姿绳索划船" -> R.drawable.asset_exercise_seated_cable_row
        "杠铃卧推" -> R.drawable.asset_exercise_barbell_bench_press
        "器械推胸" -> R.drawable.asset_exercise_machine_chest_press
        "器械腿屈伸" -> R.drawable.asset_exercise_leg_extension
        else -> null
    }

    @DrawableRes
    fun cameraGuide(type: CameraGuide): Int = when (type) {
        CameraGuide.Front -> R.drawable.asset_camera_angle_front
        CameraGuide.Front45 -> R.drawable.asset_camera_angle_front_45
        CameraGuide.Side45 -> R.drawable.asset_camera_angle_side_45
    }

    @DrawableRes
    fun errorIllustration(tag: String): Int? = when (tag) {
        "膝内扣" -> R.drawable.asset_error_squat_knee_valgus
        "深度不足" -> R.drawable.asset_error_squat_depth_insufficient
        "躯干前倾过多" -> R.drawable.asset_error_squat_depth_insufficient
        "借力摆动" -> R.drawable.asset_error_lateral_raise_shrugging
        "抬手高度不足" -> R.drawable.asset_error_lateral_raise_asymmetry
        "左右不对称" -> R.drawable.asset_error_lateral_raise_asymmetry
        "推举高度不足" -> R.drawable.asset_error_shoulder_press_unstable
        "左右不稳定" -> R.drawable.asset_error_shoulder_press_unstable
        "手臂路径前飘" -> R.drawable.asset_error_shoulder_press_path_forward
        "下拉不充分" -> R.drawable.asset_error_lat_pulldown_incomplete
        "左右发力不一致" -> R.drawable.asset_error_lat_pulldown_incomplete
        "肘部收得不够" -> R.drawable.asset_error_row_pull_short
        "拉肘不充分" -> R.drawable.asset_error_row_pull_short
        "回拉不紧" -> R.drawable.asset_error_row_pull_short
        "上推不充分" -> R.drawable.asset_error_bench_unstable_path
        "肘外展过多" -> R.drawable.asset_error_bench_elbow_flare
        "左右路径不稳" -> R.drawable.asset_error_bench_unstable_path
        "推起不充分" -> R.drawable.asset_error_bench_unstable_path
        "肘部展开过多" -> R.drawable.asset_error_bench_elbow_flare
        "伸膝不充分" -> R.drawable.asset_error_leg_extension_incomplete
        "双腿节奏不一致" -> R.drawable.asset_error_leg_extension_async
        else -> null
    }
}

enum class CameraGuide {
    Front,
    Front45,
    Side45,
}

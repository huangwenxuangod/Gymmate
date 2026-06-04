package com.gymmate.app.training

data class ActionRuleProfile(
    val name: String,
    val cameraView: String,
    val requiredLandmarks: List<String>,
    val startCue: String,
    val endCue: String,
    val countCue: String,
    val hardFails: List<String>,
    val softWarnings: List<String>,
)

object ActionRules {
    val profiles = listOf(
        ActionRuleProfile(
            name = "杠铃深蹲",
            cameraView = "侧前 45°",
            requiredLandmarks = listOf("left_hip", "right_hip", "left_knee", "right_knee", "left_ankle", "right_ankle"),
            startCue = "站直并保持膝、髋、踝完整入镜",
            endCue = "下蹲到底，膝角进入 95° 到 110°",
            countCue = "站立 -> 底部 -> 重新站直算 1 次",
            hardFails = listOf("深度不足"),
            softWarnings = listOf("膝内扣", "躯干前倾过多"),
        ),
        ActionRuleProfile(
            name = "哑铃推肩",
            cameraView = "正前",
            requiredLandmarks = listOf("left_shoulder", "right_shoulder", "left_elbow", "right_elbow", "left_wrist", "right_wrist", "left_ear", "right_ear"),
            startCue = "手腕回到肩旁准备位",
            endCue = "手腕过耳线，肘接近伸直",
            countCue = "肩旁 -> 头顶 -> 回肩旁算 1 次",
            hardFails = listOf("推举高度不足"),
            softWarnings = listOf("左右不稳定", "手臂路径前飘"),
        ),
        ActionRuleProfile(
            name = "哑铃侧平举",
            cameraView = "正前",
            requiredLandmarks = listOf("left_shoulder", "right_shoulder", "left_elbow", "right_elbow", "left_wrist", "right_wrist", "left_hip", "right_hip"),
            startCue = "手落髋侧",
            endCue = "抬到肩高附近",
            countCue = "髋侧 -> 肩高 -> 回髋侧算 1 次",
            hardFails = listOf("抬手高度不足"),
            softWarnings = listOf("左右不对称", "借力摆动"),
        ),
        ActionRuleProfile(
            name = "坐姿绳索划船",
            cameraView = "前方 45°",
            requiredLandmarks = listOf("left_shoulder", "right_shoulder", "left_elbow", "right_elbow", "left_wrist", "right_wrist"),
            startCue = "手臂前伸",
            endCue = "肘回拉到躯干后方",
            countCue = "前伸 -> 回拉 -> 前伸算 1 次",
            hardFails = listOf("拉肘不充分"),
            softWarnings = listOf("左右不对称", "回拉不紧"),
        ),
        ActionRuleProfile(
            name = "高位下拉",
            cameraView = "前方 45°",
            requiredLandmarks = listOf("left_shoulder", "right_shoulder", "left_elbow", "right_elbow", "left_wrist", "right_wrist"),
            startCue = "手臂完全上举",
            endCue = "拉到上胸附近并收肘",
            countCue = "完全伸展 -> 下拉 -> 回完全伸展算 1 次",
            hardFails = listOf("下拉不充分"),
            softWarnings = listOf("肘部收得不够", "左右发力不一致"),
        ),
        ActionRuleProfile(
            name = "杠铃卧推",
            cameraView = "前方 45°",
            requiredLandmarks = listOf("left_shoulder", "right_shoulder", "left_elbow", "right_elbow", "left_wrist", "right_wrist"),
            startCue = "顶部或底部终点稳定",
            endCue = "到达另一终点",
            countCue = "顶部 <-> 底部闭环算 1 次",
            hardFails = listOf("ROM 不完整"),
            softWarnings = listOf("肘外展过多", "左右路径不稳"),
        ),
        ActionRuleProfile(
            name = "器械推胸",
            cameraView = "前方 45°",
            requiredLandmarks = listOf("left_shoulder", "right_shoulder", "left_elbow", "right_elbow", "left_wrist", "right_wrist"),
            startCue = "手柄回到底部",
            endCue = "推到顶端",
            countCue = "底部 -> 顶部 -> 底部算 1 次",
            hardFails = listOf("推起不充分"),
            softWarnings = listOf("左右不稳定", "肘部展开过多"),
        ),
        ActionRuleProfile(
            name = "器械腿屈伸",
            cameraView = "侧前 45°",
            requiredLandmarks = listOf("left_hip", "right_hip", "left_knee", "right_knee", "left_ankle", "right_ankle"),
            startCue = "屈膝位稳定",
            endCue = "膝接近伸直",
            countCue = "屈膝 -> 伸直到顶 -> 回屈膝算 1 次",
            hardFails = listOf("伸膝不充分"),
            softWarnings = listOf("双腿节奏不一致"),
        ),
    )

    private val profileMap = profiles.associateBy { it.name }

    fun profileOf(name: String): ActionRuleProfile? = profileMap[name]

    fun requiredPoints(name: String): List<String> = profileOf(name)?.requiredLandmarks.orEmpty()
}

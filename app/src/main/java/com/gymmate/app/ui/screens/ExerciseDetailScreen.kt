package com.gymmate.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.PriorityHigh
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.gymmate.app.data.ApiRepository
import com.gymmate.app.data.Exercise
import com.gymmate.app.data.ExerciseRuleConfig
import com.gymmate.app.ui.AppAssets
import com.gymmate.app.ui.CameraGuide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ExerciseDetailScreen(
    exerciseId: Int,
    onBack: () -> Unit,
    onStartTraining: (Int) -> Unit,
) {
    val repository = remember { ApiRepository() }
    var exercise by remember { mutableStateOf<Exercise?>(null) }
    var exerciseRule by remember { mutableStateOf<ExerciseRuleConfig?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(exerciseId) {
        runCatching {
            withContext(Dispatchers.IO) {
                Pair(
                    repository.getExercise(exerciseId),
                    runCatching { repository.getExerciseRuleById(exerciseId) }.getOrNull(),
                )
            }
        }.onSuccess { (exerciseData, ruleData) ->
            exercise = exerciseData
            exerciseRule = ruleData
        }
        loading = false
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (loading) {
            CircularProgressIndicator()
        } else {
            val data = exercise ?: return@Surface
            androidx.compose.foundation.lazy.LazyColumn(
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Card(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            AsyncImage(
                                model = AppAssets.exerciseCover(data.name) ?: data.mediaUrl,
                                contentDescription = data.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(250.dp),
                            )
                            Column(
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(data.name, style = MaterialTheme.typography.headlineMedium)
                                Text(
                                    data.description,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    DetailChip(Icons.Outlined.FitnessCenter, data.category)
                                    DetailChip(Icons.Outlined.Visibility, cameraGuideLabel(data.name))
                                    DetailChip(Icons.Outlined.PlayArrow, difficultyLabel(data.difficultyLevel))
                                }
                            }
                        }
                    }
                }
                item {
                    SimpleSectionCard(
                        title = "目标肌群",
                        body = data.targetMuscles.joinToString(" / "),
                    )
                }
                item {
                    GuideVisualCard(
                        title = "推荐机位",
                        body = exerciseRule?.let {
                            "${it.camera_view}｜${it.start_cue}"
                        } ?: cameraGuideBody(data.name),
                        imageRes = AppAssets.cameraGuide(
                            exerciseRule?.camera_view?.toCameraGuide() ?: cameraGuideForExercise(data.name),
                        ),
                    )
                }
                item {
                    RuleSectionCard(rule = exerciseRule, fallbackName = data.name)
                }
                item {
                    SimpleSectionCard(
                        title = "标准动作",
                        body = data.standardSteps.mapIndexed { index, text -> "${index + 1}. $text" }.joinToString("\n"),
                    )
                }
                item {
                    SimpleSectionCard(
                        title = "高频错误",
                        body = data.commonErrors.joinToString("\n"),
                    )
                }
                item {
                    SimpleSectionCard(
                        title = "纠正建议",
                        body = data.correctionTips.joinToString("\n"),
                    )
                }
                item {
                    Button(
                        onClick = { onStartTraining(data.id) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("进入训练")
                    }
                }
                item {
                    Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                        Text("返回")
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SimpleSectionCard(title: String, body: String) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun GuideVisualCard(title: String, body: String, imageRes: Int) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AsyncImage(
                model = imageRes,
                contentDescription = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
            )
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(body, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun RuleSectionCard(rule: ExerciseRuleConfig?, fallbackName: String) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("识别规则", style = MaterialTheme.typography.titleLarge)
            if (rule == null) {
                Text(
                    "当前先展示基础动作说明，$fallbackName 的详细视觉判定规则暂未拉取成功。",
                    style = MaterialTheme.typography.bodyMedium,
                )
                return@Column
            }

            DetailChip(Icons.Outlined.Visibility, "机位 ${rule.camera_view}")
            DetailChip(Icons.Outlined.PlayArrow, "计数 ${rule.count_cue}")
            DetailChip(Icons.Outlined.Flag, "开始 ${rule.start_cue}")
            DetailChip(Icons.Outlined.Flag, "结束 ${rule.end_cue}")
            DetailChip(Icons.Outlined.FitnessCenter, "关键点 ${rule.required_landmarks.joinToString(" / ")}")

            if (rule.hard_fails.isNotEmpty()) {
                Text("必须避免", style = MaterialTheme.typography.titleMedium)
                Text(rule.hard_fails.joinToString("\n") { "• $it" }, style = MaterialTheme.typography.bodyMedium)
            }
            if (rule.soft_warnings.isNotEmpty()) {
                Text("优先修正", style = MaterialTheme.typography.titleMedium)
                Text(rule.soft_warnings.joinToString("\n") { "• $it" }, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun cameraGuideForExercise(name: String): CameraGuide = when (name) {
    "杠铃深蹲" -> CameraGuide.Side45
    "高位下拉", "坐姿绳索划船", "杠铃卧推", "器械推胸" -> CameraGuide.Front45
    else -> CameraGuide.Front
}

private fun cameraGuideLabel(name: String): String = when (cameraGuideForExercise(name)) {
    CameraGuide.Front -> "正前方"
    CameraGuide.Front45 -> "前方 45°"
    CameraGuide.Side45 -> "侧前 45°"
}

private fun cameraGuideBody(name: String): String = when (cameraGuideForExercise(name)) {
    CameraGuide.Front -> "适合推肩、侧平举等左右对称动作，确保肩、肘、腕都在画面里。"
    CameraGuide.Front45 -> "适合上肢推拉动作，既能看到左右对称，也能看到路径变化。"
    CameraGuide.Side45 -> "适合深蹲类动作，更容易看清髋、膝、踝和下蹲深度。"
}

private fun difficultyLabel(level: String): String = when (level.lowercase()) {
    "beginner" -> "新手友好"
    "intermediate" -> "轻进阶"
    else -> level
}

private fun String.toCameraGuide(): CameraGuide = when {
    contains("侧前") -> CameraGuide.Side45
    contains("45") -> CameraGuide.Front45
    else -> CameraGuide.Front
}

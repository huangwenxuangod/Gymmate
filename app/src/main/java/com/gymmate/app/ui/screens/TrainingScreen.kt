package com.gymmate.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.gymmate.app.data.ApiRepository
import com.gymmate.app.data.Exercise
import com.gymmate.app.data.ExerciseRuleConfig
import com.gymmate.app.data.FeedbackResponse
import com.gymmate.app.training.CameraPoseAnalyzer
import com.gymmate.app.training.FormEngine
import com.gymmate.app.training.PoseDetectionState
import com.gymmate.app.training.PoseFrame
import com.gymmate.app.training.PoseLandmarkerBridge
import com.gymmate.app.ui.AppAssets
import com.gymmate.app.ui.CameraGuide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun TrainingScreen(
    exerciseId: Int,
    authToken: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val repository = remember { ApiRepository() }
    val formEngine = remember { FormEngine() }
    val poseBridge = remember { PoseLandmarkerBridge(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    var exercise by remember { mutableStateOf<Exercise?>(null) }
    var exerciseRule by remember { mutableStateOf<ExerciseRuleConfig?>(null) }
    var loading by remember { mutableStateOf(true) }
    var cameraGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var repCount by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var confidence by remember { mutableIntStateOf(0) }
    var stage by remember { mutableStateOf("准备") }
    var status by remember { mutableStateOf("先让全身进入画面") }
    var errorTags by remember { mutableStateOf(listOf<String>()) }
    var sessionId by remember { mutableIntStateOf(0) }
    var finishing by remember { mutableStateOf(false) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var poseState by remember { mutableStateOf(PoseDetectionState()) }
    var resultFeedback by remember { mutableStateOf<FeedbackResponse?>(null) }
    var trainingActive by remember { mutableStateOf(false) }
    var hasCreatedSession by remember { mutableStateOf(false) }
    var showAssist by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        cameraGranted = granted
    }

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

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            poseBridge.close()
        }
    }

    LaunchedEffect(trainingActive, exercise?.id, authToken) {
        val data = exercise ?: return@LaunchedEffect
        if (!trainingActive || hasCreatedSession || authToken.isBlank()) return@LaunchedEffect
        sessionId = withContext(Dispatchers.IO) {
            repository.createSession(authToken, data.id, Instant.now().toString())
        }
        hasCreatedSession = true
    }

    LaunchedEffect(cameraGranted, previewView, exercise?.name) {
        val previewTarget = previewView ?: return@LaunchedEffect
        val exerciseName = exercise?.name ?: return@LaunchedEffect
        if (!cameraGranted) return@LaunchedEffect
        bindCamera(
            previewView = previewTarget,
            lifecycleOwner = lifecycleOwner,
            context = context,
            cameraExecutor = cameraExecutor,
            poseLandmarkerBridge = poseBridge,
            onPoseDetected = { detectionState: PoseDetectionState, poseFrame: PoseFrame? ->
                poseState = detectionState

                if (!trainingActive) {
                    status = when {
                        poseBridge.poseLandmarker == null -> "模型未加载，请放入 task 文件"
                        detectionState.points.isEmpty() -> "先站到完整视野里"
                        else -> "站位已就绪，可以开始"
                    }
                    confidence = if (detectionState.points.isEmpty()) 0 else 100
                    return@bindCamera
                }

                val feedback = if (poseBridge.poseLandmarker == null) {
                    formEngine.process(exerciseName, null)
                } else {
                    formEngine.process(exerciseName, poseFrame)
                }
                repCount = feedback.repCount
                score = feedback.score
                errorTags = feedback.errorTags
                stage = feedback.stage
                confidence = feedback.confidence
                status = when {
                    poseBridge.poseLandmarker == null -> "模型未加载，请放入 task 文件"
                    detectionState.points.isEmpty() -> "正在寻找身体关键点"
                    else -> feedback.status
                }
            },
        )
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val data = exercise ?: return@Surface
            val primaryError = errorTags.firstOrNull()

            Box(modifier = Modifier.fillMaxSize()) {
                if (cameraGranted) {
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).also { previewView = it }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                    PoseOverlay(points = poseState.points)
                } else {
                    AsyncImage(
                        model = AppAssets.emptyCameraPermission,
                        contentDescription = "相机权限",
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.42f),
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.58f),
                                ),
                            ),
                        ),
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    TopHudRow(
                        title = data.name,
                        trainingActive = trainingActive,
                        onBack = onBack,
                        onAssist = { showAssist = true },
                        onReset = {
                            formEngine.reset()
                            repCount = 0
                            score = 0
                            confidence = 0
                            stage = "准备"
                            status = "已重置，请重新站位"
                            errorTags = emptyList()
                            trainingActive = false
                        },
                    )
                    StatusCapsules(
                        stage = stage,
                        status = when {
                            primaryError != null && trainingActive -> "先修正：$primaryError"
                            else -> status
                        },
                        confidence = confidence,
                    )
                }

                CenterMetric(
                    repCount = repCount,
                    score = score,
                    trainingActive = trainingActive,
                    modifier = Modifier.align(Alignment.Center),
                )

                BottomHud(
                    cameraGranted = cameraGranted,
                    trainingActive = trainingActive,
                    canFinish = hasCreatedSession && sessionId != 0,
                    primaryError = primaryError,
                    onGrant = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    onStart = {
                        trainingActive = true
                        status = "开始采集动作"
                    },
                    onFinish = { finishing = true },
                    onClose = onBack,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )

                if (finishing && sessionId != 0) {
                    LaunchedEffect(finishing) {
                        val feedback = withContext(Dispatchers.IO) {
                            repository.getFeedback(
                                exerciseName = data.name,
                                avgScore = score,
                                errorTags = errorTags,
                                focus = status,
                            )
                        }
                        withContext(Dispatchers.IO) {
                            repository.finishSession(
                                sessionId = sessionId,
                                token = authToken,
                                totalReps = repCount,
                                avgScore = score,
                                errorTags = errorTags.ifEmpty { feedback.topErrors },
                                summaryText = feedback.summary,
                                endedAt = Instant.now().toString(),
                            )
                        }
                        finishing = false
                        trainingActive = false
                        status = feedback.coachTip
                        resultFeedback = feedback
                    }
                }
            }
        }
    }

    if (showAssist) {
        val data = exercise
        if (data != null) {
            AssistDialog(
                exerciseName = data.name,
                exerciseRule = exerciseRule,
                primaryError = errorTags.firstOrNull(),
                onDismiss = { showAssist = false },
            )
        }
    }

    if (resultFeedback != null) {
        TrainingResultDialog(
            exerciseName = exercise?.name.orEmpty(),
            repCount = repCount,
            score = score,
            errorTags = errorTags.ifEmpty { resultFeedback?.topErrors.orEmpty() },
            feedback = resultFeedback!!,
            onDismiss = { resultFeedback = null },
        )
    }
}

@Composable
private fun TopHudRow(
    title: String,
    trainingActive: Boolean,
    onBack: () -> Unit,
    onAssist: () -> Unit,
    onReset: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            HudIconButton(
                icon = Icons.Outlined.ArrowBack,
                contentDescription = "返回",
                onClick = onBack,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (trainingActive) "训练中" else "准备开始",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (trainingActive) {
                HudIconButton(
                    icon = Icons.Outlined.Info,
                    contentDescription = "辅助",
                    onClick = onAssist,
                )
            } else {
                HudIconButton(
                    icon = Icons.Outlined.Refresh,
                    contentDescription = "重置",
                    onClick = onReset,
                )
            }
        }
    }
}

@Composable
private fun StatusCapsules(
    stage: String,
    status: String,
    confidence: Int,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SoftCapsule(label = stage)
            SoftCapsule(label = "置信度 $confidence%")
        }
        Text(
            text = status,
            color = Color.White.copy(alpha = 0.92f),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun CenterMetric(
    repCount: Int,
    score: Int,
    trainingActive: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = repCount.toString(),
            color = Color.White,
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = MaterialTheme.typography.headlineLarge.fontSize * 3f),
        )
        Text(
            text = if (trainingActive) "当前次数" else "准备就绪后开始",
            color = Color.White.copy(alpha = 0.75f),
            style = MaterialTheme.typography.bodyLarge,
        )
        SoftCapsule(label = "评分 $score")
    }
}

@Composable
private fun BottomHud(
    cameraGranted: Boolean,
    trainingActive: Boolean,
    canFinish: Boolean,
    primaryError: String?,
    onGrant: () -> Unit,
    onStart: () -> Unit,
    onFinish: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (primaryError != null && trainingActive) {
            Text(
                text = primaryError,
                color = Color(0xFFFFE7B0),
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        if (!cameraGranted) {
            LargePillButton(
                icon = Icons.Outlined.Cameraswitch,
                label = "开启相机",
                filled = true,
                onClick = onGrant,
            )
            Text(
                text = "打开权限后就能开始识别",
                color = Color.White.copy(alpha = 0.72f),
                style = MaterialTheme.typography.bodyMedium,
            )
            return
        }

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            if (!trainingActive) {
                LargeCircleAction(
                    icon = Icons.Outlined.PlayArrow,
                    label = "开始",
                    filled = true,
                    onClick = onStart,
                )
            } else {
                LargeCircleAction(
                    icon = Icons.Outlined.Stop,
                    label = "停止",
                    filled = true,
                    filledColor = Color(0xFFE45B5B),
                    enabled = canFinish,
                    onClick = onFinish,
                )
            }
        }

        when {
            !trainingActive -> {
                Text(
                    text = "站位就绪后开始",
                    color = Color.White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "关闭",
                    modifier = Modifier.clickable(onClick = onClose),
                    color = Color(0xFFFF7D7D),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            !canFinish -> {
                Text(
                    text = "正在建立训练记录",
                    color = Color.White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun LargeCircleAction(
    icon: ImageVector,
    label: String,
    filled: Boolean,
    filledColor: Color = Color(0xFF9FD3A8),
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val background = if (filled) filledColor else Color.White.copy(alpha = 0.12f)
    val content = if (filled) Color(0xFF0F1711) else Color.White
    val alpha = if (enabled) 1f else 0.45f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(78.dp)
                .clip(CircleShape)
                .background(background.copy(alpha = alpha))
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = content.copy(alpha = alpha),
                modifier = Modifier.size(32.dp),
            )
        }
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.92f * alpha),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun LargePillButton(
    icon: ImageVector,
    label: String,
    filled: Boolean,
    onClick: () -> Unit,
) {
    val background = if (filled) Color(0xFF9FD3A8) else Color.White.copy(alpha = 0.12f)
    val content = if (filled) Color(0xFF0F1711) else Color.White

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, contentDescription = label, tint = content)
        Text(label, color = content, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun HudIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.12f)),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
        )
    }
}

@Composable
private fun SoftCapsule(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.14f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.9f),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun AssistDialog(
    exerciseName: String,
    exerciseRule: ExerciseRuleConfig?,
    primaryError: String?,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("知道了")
            }
        },
        title = { Text("动作辅助") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AsyncImage(
                    model = primaryError?.let { AppAssets.errorIllustration(it) }
                        ?: AppAssets.cameraGuide(
                            exerciseRule?.camera_view?.toTrainingCameraGuide() ?: trainingCameraGuide(exerciseName),
                        ),
                    contentDescription = "辅助提示",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(20.dp)),
                )
                Text(
                    text = primaryError?.let { "先修正 $it，再追求次数和速度。" }
                        ?: buildAssistBody(exerciseName, exerciseRule),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
    )
}

@Composable
private fun TrainingResultDialog(
    exerciseName: String,
    repCount: Int,
    score: Int,
    errorTags: List<String>,
    feedback: FeedbackResponse,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("完成")
            }
        },
        title = { Text("$exerciseName 训练结果", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("次数：$repCount")
                Text("评分：$score")
                if (errorTags.isNotEmpty()) {
                    Text("主错误：${errorTags.first()}")
                }
                Text("总结：${feedback.summary}")
                Text("下次重点：${feedback.nextTimeFocus}")
            }
        },
    )
}

private suspend fun bindCamera(
    previewView: PreviewView,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    context: android.content.Context,
    cameraExecutor: ExecutorService,
    poseLandmarkerBridge: PoseLandmarkerBridge,
    onPoseDetected: (PoseDetectionState, PoseFrame?) -> Unit,
) {
    val cameraProvider = ProcessCameraProvider.getInstance(context).get()
    val preview = Preview.Builder().build().also {
        it.surfaceProvider = previewView.surfaceProvider
    }

    val analysis = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
        .build()
        .also {
            it.setAnalyzer(
                cameraExecutor,
                CameraPoseAnalyzer(
                    poseLandmarkerBridge = poseLandmarkerBridge,
                    onPoseDetected = onPoseDetected,
                ),
            )
        }

    val selector = CameraSelector.DEFAULT_FRONT_CAMERA
    cameraProvider.unbindAll()
    cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
}

private fun trainingCameraGuide(name: String): CameraGuide = when (name) {
    "杠铃深蹲" -> CameraGuide.Side45
    "高位下拉", "坐姿绳索划船", "杠铃卧推", "器械推胸" -> CameraGuide.Front45
    else -> CameraGuide.Front
}

private fun trainingCameraGuideBody(name: String): String = when (trainingCameraGuide(name)) {
    CameraGuide.Front -> "正前方适合推肩、侧平举等左右对称动作。"
    CameraGuide.Front45 -> "前方 45 度更适合推拉路径类动作。"
    CameraGuide.Side45 -> "侧前 45 度更适合深蹲类动作，看深度更稳定。"
}

private fun buildAssistBody(name: String, rule: ExerciseRuleConfig?): String {
    if (rule == null) return trainingCameraGuideBody(name)
    val warnings = buildList {
        add("机位：${rule.camera_view}")
        add("开始：${rule.start_cue}")
        add("结束：${rule.end_cue}")
        add("计数：${rule.count_cue}")
        if (rule.hard_fails.isNotEmpty()) add("必须避免：${rule.hard_fails.joinToString("、")}")
        if (rule.soft_warnings.isNotEmpty()) add("优先修正：${rule.soft_warnings.joinToString("、")}")
    }
    return warnings.joinToString("\n")
}

private fun String.toTrainingCameraGuide(): CameraGuide = when {
    contains("侧前") -> CameraGuide.Side45
    contains("45") -> CameraGuide.Front45
    else -> CameraGuide.Front
}

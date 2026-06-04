package com.gymmate.app.training

import kotlin.math.abs

data class PosePoint(
    val x: Float,
    val y: Float,
    val z: Float = 0f,
    val score: Float,
)

data class PoseFrame(
    val landmarks: Map<String, PosePoint>,
)

data class RepetitionFeedback(
    val repCount: Int,
    val score: Int,
    val errorTags: List<String>,
    val status: String,
    val stage: String,
    val confidence: Int,
)

class FormEngine {
    private val detectors = mapOf(
        "杠铃深蹲" to SquatDetector(),
        "哑铃侧平举" to LateralRaiseDetector(),
        "哑铃推肩" to ShoulderPressDetector(),
        "坐姿绳索划船" to SeatedRowDetector(),
        "高位下拉" to LatPulldownDetector(),
        "杠铃卧推" to BenchPressDetector(),
        "器械推胸" to ChestPressDetector(),
        "器械腿屈伸" to LegExtensionDetector(),
    )

    private var lastExerciseName = ""

    fun reset() {
        detectors.values.forEach { it.reset() }
        lastExerciseName = ""
    }

    fun process(exerciseName: String, frame: PoseFrame?): RepetitionFeedback {
        if (exerciseName != lastExerciseName) {
            detectors.values.forEach { it.reset() }
            lastExerciseName = exerciseName
        }
        if (frame == null || frame.landmarks.isEmpty()) {
            return DetectorSupport.emptyFeedback(status = "等待识别人体关键点")
        }

        val visibility = DetectorSupport.averageConfidence(frame, ActionRules.requiredPoints(exerciseName))
        if (visibility < 0.58f) {
            return DetectorSupport.emptyFeedback(
                status = "站到完整视野里，保持关键关节都入镜",
                confidence = (visibility * 100).toInt(),
            )
        }

        val detector = detectors[exerciseName]
        if (detector != null) {
            return detector.process(frame, visibility)
        }

        return RepetitionFeedback(
            repCount = 0,
            score = 78,
            errorTags = listOf("规则开发中"),
            status = "已识别骨架，动作规则待补充",
            stage = "识别中",
            confidence = (visibility * 100).toInt(),
        )
    }
}

private class SquatDetector : ExerciseDetector {
    private var repCount = 0
    private var phase = MotionPhase.START
    private var stableFrames = 0
    private var smoothedDepth = 0f

    override fun reset() {
        repCount = 0
        phase = MotionPhase.START
        stableFrames = 0
        smoothedDepth = 0f
    }

    override fun process(frame: PoseFrame, visibility: Float): RepetitionFeedback {
        stableFrames += 1
        val leftHip = frame.point("left_hip") ?: return DetectorSupport.waiting(repCount, visibility)
        val rightHip = frame.point("right_hip") ?: return DetectorSupport.waiting(repCount, visibility)
        val leftKnee = frame.point("left_knee") ?: return DetectorSupport.waiting(repCount, visibility)
        val rightKnee = frame.point("right_knee") ?: return DetectorSupport.waiting(repCount, visibility)
        val leftAnkle = frame.point("left_ankle") ?: return DetectorSupport.waiting(repCount, visibility)
        val rightAnkle = frame.point("right_ankle") ?: return DetectorSupport.waiting(repCount, visibility)

        val kneeAngle = DetectorSupport.averageAngle(leftHip, leftKnee, leftAnkle, rightHip, rightKnee, rightAnkle)
        val torsoLean = abs(
            DetectorSupport.mid(frame.point("left_shoulder") ?: leftHip, frame.point("right_shoulder") ?: rightHip).x -
                DetectorSupport.mid(leftHip, rightHip).x,
        )
        val hipDepth = DetectorSupport.mid(leftHip, rightHip).y - DetectorSupport.mid(leftKnee, rightKnee).y
        smoothedDepth = if (smoothedDepth == 0f) hipDepth else smoothedDepth * 0.72f + hipDepth * 0.28f

        if (kneeAngle < 105f && phase != MotionPhase.BOTTOM && stableFrames > 4) {
            phase = MotionPhase.BOTTOM
        } else if (kneeAngle > 156f && phase == MotionPhase.BOTTOM && stableFrames > 4) {
            repCount += 1
            phase = MotionPhase.TOP
        } else if (kneeAngle in 105f..150f) {
            phase = if (smoothedDepth > -0.02f) MotionPhase.DESCENDING else MotionPhase.ASCENDING
        }

        val tags = buildList {
            if (kneeAngle > 118f) add("深度不足")
            if (abs(leftKnee.x - rightKnee.x) < abs(leftAnkle.x - rightAnkle.x) * 0.78f) add("膝内扣")
            if (torsoLean > 0.11f) add("躯干前倾过多")
        }
        return DetectorSupport.result(
            repCount = repCount,
            visibility = visibility,
            tags = tags,
            status = when {
                visibility < 0.7f -> "保持全身入镜，膝、髋、踝要看清"
                tags.isEmpty() -> "深蹲节奏稳定，可以继续下蹲到底再起身"
                "膝内扣" in tags -> "注意膝盖方向跟脚尖一致"
                "躯干前倾过多" in tags -> "收紧核心，起身时把胸口带起来"
                else -> "下蹲还可以再深一点"
            },
            stage = when (phase) {
                MotionPhase.BOTTOM -> "底部"
                MotionPhase.TOP -> "顶部"
                MotionPhase.DESCENDING -> "下蹲中"
                MotionPhase.ASCENDING -> "起身中"
                else -> "准备"
            },
        )
    }
}

private class LateralRaiseDetector : ExerciseDetector {
    private var repCount = 0
    private var phase = MotionPhase.START
    private var stableFrames = 0

    override fun reset() {
        repCount = 0
        phase = MotionPhase.START
        stableFrames = 0
    }

    override fun process(frame: PoseFrame, visibility: Float): RepetitionFeedback {
        stableFrames += 1
        val leftShoulder = frame.point("left_shoulder") ?: return DetectorSupport.waiting(repCount, visibility)
        val rightShoulder = frame.point("right_shoulder") ?: return DetectorSupport.waiting(repCount, visibility)
        val leftElbow = frame.point("left_elbow") ?: return DetectorSupport.waiting(repCount, visibility)
        val rightElbow = frame.point("right_elbow") ?: return DetectorSupport.waiting(repCount, visibility)
        val leftWrist = frame.point("left_wrist") ?: return DetectorSupport.waiting(repCount, visibility)
        val rightWrist = frame.point("right_wrist") ?: return DetectorSupport.waiting(repCount, visibility)
        val leftHip = frame.point("left_hip") ?: return DetectorSupport.waiting(repCount, visibility)
        val rightHip = frame.point("right_hip") ?: return DetectorSupport.waiting(repCount, visibility)

        val armAngle = DetectorSupport.averageAngle(leftHip, leftShoulder, leftElbow, rightHip, rightShoulder, rightElbow)
        val wristHeight = (leftWrist.y + rightWrist.y) / 2f
        val shoulderHeight = (leftShoulder.y + rightShoulder.y) / 2f
        val hipHeight = (leftHip.y + rightHip.y) / 2f

        if (armAngle > 72f && phase != MotionPhase.TOP && stableFrames > 4) {
            phase = MotionPhase.TOP
        } else if (wristHeight >= hipHeight - 0.03f && phase == MotionPhase.TOP && stableFrames > 4) {
            repCount += 1
            phase = MotionPhase.START
        } else if (wristHeight < hipHeight - 0.03f && wristHeight > shoulderHeight - 0.04f) {
            phase = MotionPhase.ASCENDING
        }

        val tags = buildList {
            if (armAngle < 62f) add("抬手高度不足")
            if (abs(leftWrist.y - rightWrist.y) > 0.06f) add("左右不对称")
            if (abs(leftElbow.y - rightElbow.y) > 0.06f) add("借力摆动")
        }
        return DetectorSupport.result(
            repCount = repCount,
            visibility = visibility,
            tags = tags,
            status = when {
                tags.isEmpty() -> "侧平举高度不错，保持肩膀别耸起来"
                "左右不对称" in tags -> "两侧抬起的速度和终点尽量一致"
                else -> "抬到接近肩高就够了，不要靠惯性甩起来"
            },
            stage = when (phase) {
                MotionPhase.TOP -> "顶峰"
                MotionPhase.ASCENDING -> "抬起中"
                else -> "下放中"
            },
        )
    }
}

private class ShoulderPressDetector : ExerciseDetector {
    private var repCount = 0
    private var phase = MotionPhase.START
    private var stableFrames = 0

    override fun reset() {
        repCount = 0
        phase = MotionPhase.START
        stableFrames = 0
    }

    override fun process(frame: PoseFrame, visibility: Float): RepetitionFeedback {
        stableFrames += 1
        val leftShoulder = frame.point("left_shoulder") ?: return DetectorSupport.waiting(repCount, visibility)
        val rightShoulder = frame.point("right_shoulder") ?: return DetectorSupport.waiting(repCount, visibility)
        val leftElbow = frame.point("left_elbow") ?: return DetectorSupport.waiting(repCount, visibility)
        val rightElbow = frame.point("right_elbow") ?: return DetectorSupport.waiting(repCount, visibility)
        val leftWrist = frame.point("left_wrist") ?: return DetectorSupport.waiting(repCount, visibility)
        val rightWrist = frame.point("right_wrist") ?: return DetectorSupport.waiting(repCount, visibility)
        val leftEar = frame.point("left_ear") ?: return DetectorSupport.waiting(repCount, visibility)
        val rightEar = frame.point("right_ear") ?: return DetectorSupport.waiting(repCount, visibility)

        val elbowAngle = DetectorSupport.averageAngle(leftShoulder, leftElbow, leftWrist, rightShoulder, rightElbow, rightWrist)
        val wristHeight = (leftWrist.y + rightWrist.y) / 2f
        val earHeight = (leftEar.y + rightEar.y) / 2f
        val shoulderHeight = (leftShoulder.y + rightShoulder.y) / 2f

        if (elbowAngle > 150f && wristHeight < earHeight && stableFrames > 4) {
            phase = MotionPhase.TOP
        } else if (wristHeight > shoulderHeight && phase == MotionPhase.TOP && stableFrames > 4) {
            repCount += 1
            phase = MotionPhase.START
        } else if (wristHeight <= shoulderHeight) {
            phase = MotionPhase.PRESSING
        }

        val tags = buildList {
            if (elbowAngle < 145f) add("推举高度不足")
            if (abs(leftWrist.y - rightWrist.y) > 0.06f) add("左右不稳定")
            if ((leftWrist.x + rightWrist.x) / 2f < (leftShoulder.x + rightShoulder.x) / 2f - 0.08f) add("手臂路径前飘")
        }
        return DetectorSupport.result(
            repCount = repCount,
            visibility = visibility,
            tags = tags,
            status = when {
                tags.isEmpty() -> "推肩路线稳定，顶峰把手臂伸长一点"
                "手臂路径前飘" in tags -> "往头顶正上方推，不要往前顶"
                else -> "两边节奏再统一一些"
            },
            stage = when (phase) {
                MotionPhase.TOP -> "顶峰"
                MotionPhase.PRESSING -> "上推中"
                else -> "下放中"
            },
        )
    }
}

private class SeatedRowDetector : ExerciseDetector {
    private var repCount = 0
    private var phase = MotionPhase.START
    private var stableFrames = 0

    override fun reset() {
        repCount = 0
        phase = MotionPhase.START
        stableFrames = 0
    }

    override fun process(frame: PoseFrame, visibility: Float): RepetitionFeedback {
        stableFrames += 1
        val leftShoulder = frame.point("left_shoulder") ?: return DetectorSupport.waiting(repCount, visibility)
        val rightShoulder = frame.point("right_shoulder") ?: return DetectorSupport.waiting(repCount, visibility)
        val leftElbow = frame.point("left_elbow") ?: return DetectorSupport.waiting(repCount, visibility)
        val rightElbow = frame.point("right_elbow") ?: return DetectorSupport.waiting(repCount, visibility)
        val leftWrist = frame.point("left_wrist") ?: return DetectorSupport.waiting(repCount, visibility)
        val rightWrist = frame.point("right_wrist") ?: return DetectorSupport.waiting(repCount, visibility)

        val elbowBackDistance = ((leftShoulder.x - leftElbow.x) + (rightElbow.x - rightShoulder.x)) / 2f
        val elbowAngle = DetectorSupport.averageAngle(leftShoulder, leftElbow, leftWrist, rightShoulder, rightElbow, rightWrist)

        if (elbowBackDistance > 0.07f && elbowAngle < 105f && stableFrames > 4) {
            phase = MotionPhase.BOTTOM
        } else if (elbowBackDistance < 0.015f && phase == MotionPhase.BOTTOM && stableFrames > 4) {
            repCount += 1
            phase = MotionPhase.RETURNING
        } else {
            phase = MotionPhase.PULLING
        }

        val tags = buildList {
            if (elbowBackDistance < 0.05f) add("拉肘不充分")
            if (abs(leftElbow.y - rightElbow.y) > 0.07f) add("左右不对称")
            if (elbowAngle > 120f) add("回拉不紧")
        }
        return DetectorSupport.result(
            repCount = repCount,
            visibility = visibility,
            tags = tags,
            status = if (tags.isEmpty()) {
                "划船回拉稳定，继续先收肩胛再拉肘"
            } else {
                "把手柄再拉近躯干一点，别只用手臂"
            },
            stage = when (phase) {
                MotionPhase.BOTTOM -> "后缩位"
                MotionPhase.PULLING -> "回拉中"
                else -> "前伸中"
            },
        )
    }
}

private class LatPulldownDetector : ExerciseDetector {
    private var repCount = 0
    private var phase = MotionPhase.START
    private var stableFrames = 0

    override fun reset() {
        repCount = 0
        phase = MotionPhase.START
        stableFrames = 0
    }

    override fun process(frame: PoseFrame, visibility: Float): RepetitionFeedback {
        stableFrames += 1
        val leftElbow = frame.point("left_elbow") ?: return DetectorSupport.waiting(repCount, visibility)
        val rightElbow = frame.point("right_elbow") ?: return DetectorSupport.waiting(repCount, visibility)
        val leftShoulder = frame.point("left_shoulder") ?: return DetectorSupport.waiting(repCount, visibility)
        val rightShoulder = frame.point("right_shoulder") ?: return DetectorSupport.waiting(repCount, visibility)
        val leftWrist = frame.point("left_wrist") ?: return DetectorSupport.waiting(repCount, visibility)
        val rightWrist = frame.point("right_wrist") ?: return DetectorSupport.waiting(repCount, visibility)

        val elbowAngle = DetectorSupport.averageAngle(leftShoulder, leftElbow, leftWrist, rightShoulder, rightElbow, rightWrist)
        val wristHeight = (leftWrist.y + rightWrist.y) / 2f
        val shoulderHeight = (leftShoulder.y + rightShoulder.y) / 2f
        val bottomReached = wristHeight > shoulderHeight + 0.11f && elbowAngle < 116f
        val topReached = wristHeight < shoulderHeight - 0.12f && elbowAngle > 150f

        if (bottomReached && stableFrames > 4) {
            phase = MotionPhase.BOTTOM
        } else if (topReached && phase == MotionPhase.BOTTOM && stableFrames > 4) {
            repCount += 1
            phase = MotionPhase.TOP
        } else {
            phase = MotionPhase.PULLING
        }

        val tags = buildList {
            if (wristHeight < shoulderHeight + 0.08f) add("下拉不充分")
            if (abs(leftWrist.y - rightWrist.y) > 0.07f) add("左右发力不一致")
            if (elbowAngle > 132f) add("肘部收得不够")
        }
        return DetectorSupport.result(
            repCount = repCount,
            visibility = visibility,
            tags = tags,
            status = if (tags.isEmpty()) "下拉到底后完整还原，节奏比较稳" else "把横杆再拉低一些，再完整还原",
            stage = when (phase) {
                MotionPhase.BOTTOM -> "底部"
                MotionPhase.TOP -> "顶部"
                else -> "下拉中"
            },
        )
    }
}

private class BenchPressDetector : ExerciseDetector {
    private var repCount = 0
    private var phase = MotionPhase.TOP
    private var stableFrames = 0

    override fun reset() {
        repCount = 0
        phase = MotionPhase.TOP
        stableFrames = 0
    }

    override fun process(frame: PoseFrame, visibility: Float): RepetitionFeedback {
        stableFrames += 1
        val leftShoulder = frame.point("left_shoulder") ?: return DetectorSupport.waiting(repCount, visibility)
        val rightShoulder = frame.point("right_shoulder") ?: return DetectorSupport.waiting(repCount, visibility)
        val leftElbow = frame.point("left_elbow") ?: return DetectorSupport.waiting(repCount, visibility)
        val rightElbow = frame.point("right_elbow") ?: return DetectorSupport.waiting(repCount, visibility)
        val leftWrist = frame.point("left_wrist") ?: return DetectorSupport.waiting(repCount, visibility)
        val rightWrist = frame.point("right_wrist") ?: return DetectorSupport.waiting(repCount, visibility)

        val elbowAngle = DetectorSupport.averageAngle(leftShoulder, leftElbow, leftWrist, rightShoulder, rightElbow, rightWrist)
        val elbowWidth = abs(leftElbow.x - rightElbow.x)
        val topReached = elbowAngle > 154f
        val bottomReached = elbowAngle < 96f

        if (bottomReached && phase != MotionPhase.BOTTOM && stableFrames > 4) {
            phase = MotionPhase.BOTTOM
        } else if (topReached && phase == MotionPhase.BOTTOM && stableFrames > 4) {
            repCount += 1
            phase = MotionPhase.TOP
        } else {
            phase = MotionPhase.PRESSING
        }

        val tags = buildList {
            if (elbowAngle < 148f) add("ROM 不完整")
            if (elbowWidth > 0.52f) add("肘外展过多")
            if (abs(leftWrist.y - rightWrist.y) > 0.06f) add("左右路径不稳")
        }
        return DetectorSupport.result(
            repCount = repCount,
            visibility = visibility,
            tags = tags,
            status = if (tags.isEmpty()) "卧推路径比较顺，继续完整到底再推直" else "推到顶端并控制肘部别散得太开",
            stage = when (phase) {
                MotionPhase.BOTTOM -> "底部"
                MotionPhase.TOP -> "顶部"
                else -> "推举中"
            },
        )
    }
}

private class ChestPressDetector : ExerciseDetector {
    private var repCount = 0
    private var phase = MotionPhase.START
    private var stableFrames = 0

    override fun reset() {
        repCount = 0
        phase = MotionPhase.START
        stableFrames = 0
    }

    override fun process(frame: PoseFrame, visibility: Float): RepetitionFeedback {
        stableFrames += 1
        val leftShoulder = frame.point("left_shoulder") ?: return DetectorSupport.waiting(repCount, visibility)
        val rightShoulder = frame.point("right_shoulder") ?: return DetectorSupport.waiting(repCount, visibility)
        val leftElbow = frame.point("left_elbow") ?: return DetectorSupport.waiting(repCount, visibility)
        val rightElbow = frame.point("right_elbow") ?: return DetectorSupport.waiting(repCount, visibility)
        val leftWrist = frame.point("left_wrist") ?: return DetectorSupport.waiting(repCount, visibility)
        val rightWrist = frame.point("right_wrist") ?: return DetectorSupport.waiting(repCount, visibility)

        val elbowAngle = DetectorSupport.averageAngle(leftShoulder, leftElbow, leftWrist, rightShoulder, rightElbow, rightWrist)
        val bottomReached = elbowAngle < 98f
        val topReached = elbowAngle > 156f

        if (bottomReached && phase != MotionPhase.BOTTOM && stableFrames > 4) {
            phase = MotionPhase.BOTTOM
        } else if (topReached && phase == MotionPhase.BOTTOM && stableFrames > 4) {
            repCount += 1
            phase = MotionPhase.TOP
        } else {
            phase = MotionPhase.PRESSING
        }

        val tags = buildList {
            if (elbowAngle < 150f) add("推起不充分")
            if (abs(leftWrist.y - rightWrist.y) > 0.06f) add("左右不稳定")
            if (abs(leftElbow.x - rightElbow.x) > 0.52f) add("肘部展开过多")
        }
        return DetectorSupport.result(
            repCount = repCount,
            visibility = visibility,
            tags = tags,
            status = if (tags.isEmpty()) "推胸轨迹平稳，继续完整回程" else "推到顶端后再慢慢回程，别让手柄弹回去",
            stage = when (phase) {
                MotionPhase.BOTTOM -> "底部"
                MotionPhase.TOP -> "顶部"
                else -> "推举中"
            },
        )
    }
}

private class LegExtensionDetector : ExerciseDetector {
    private var repCount = 0
    private var phase = MotionPhase.START
    private var stableFrames = 0

    override fun reset() {
        repCount = 0
        phase = MotionPhase.START
        stableFrames = 0
    }

    override fun process(frame: PoseFrame, visibility: Float): RepetitionFeedback {
        stableFrames += 1
        val leftHip = frame.point("left_hip") ?: return DetectorSupport.waiting(repCount, visibility)
        val rightHip = frame.point("right_hip") ?: return DetectorSupport.waiting(repCount, visibility)
        val leftKnee = frame.point("left_knee") ?: return DetectorSupport.waiting(repCount, visibility)
        val rightKnee = frame.point("right_knee") ?: return DetectorSupport.waiting(repCount, visibility)
        val leftAnkle = frame.point("left_ankle") ?: return DetectorSupport.waiting(repCount, visibility)
        val rightAnkle = frame.point("right_ankle") ?: return DetectorSupport.waiting(repCount, visibility)

        val kneeAngle = DetectorSupport.averageAngle(leftHip, leftKnee, leftAnkle, rightHip, rightKnee, rightAnkle)
        val topReached = kneeAngle > 158f
        val bottomReached = kneeAngle < 108f

        if (topReached && phase != MotionPhase.TOP && stableFrames > 4) {
            phase = MotionPhase.TOP
        } else if (bottomReached && phase == MotionPhase.TOP && stableFrames > 4) {
            repCount += 1
            phase = MotionPhase.BOTTOM
        } else {
            phase = MotionPhase.PRESSING
        }

        val tags = buildList {
            if (kneeAngle < 150f) add("伸膝不充分")
            if (abs(leftAnkle.y - rightAnkle.y) > 0.07f) add("双腿节奏不一致")
        }
        return DetectorSupport.result(
            repCount = repCount,
            visibility = visibility,
            tags = tags,
            status = if (tags.isEmpty()) "腿屈伸控制不错，顶峰短暂停一下" else "顶峰伸直再停一小下，双腿保持同速",
            stage = when (phase) {
                MotionPhase.TOP -> "顶峰"
                MotionPhase.BOTTOM -> "底部"
                else -> "伸膝中"
            },
        )
    }
}

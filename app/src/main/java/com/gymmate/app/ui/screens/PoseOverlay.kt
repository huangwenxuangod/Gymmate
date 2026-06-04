package com.gymmate.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.gymmate.app.training.NormalizedPoint

private val edges = listOf(
    11 to 12,
    11 to 13,
    13 to 15,
    12 to 14,
    14 to 16,
    11 to 23,
    12 to 24,
    23 to 24,
    23 to 25,
    25 to 27,
    24 to 26,
    26 to 28,
    27 to 29,
    28 to 30,
    29 to 31,
    30 to 32,
)

@Composable
fun PoseOverlay(points: List<NormalizedPoint>) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        edges.forEach { (startIndex, endIndex) ->
            val start = points.getOrNull(startIndex) ?: return@forEach
            val end = points.getOrNull(endIndex) ?: return@forEach
            if (start.visibility < 0.4f || end.visibility < 0.4f) return@forEach

            drawLine(
                color = Color(0xFF8AE8AA),
                start = Offset(start.x * size.width, start.y * size.height),
                end = Offset(end.x * size.width, end.y * size.height),
                strokeWidth = 5f,
                cap = StrokeCap.Round,
            )
        }

        points.forEach { point ->
            if (point.visibility < 0.4f) return@forEach
            drawCircle(
                color = Color(0xFFF5F7F1),
                radius = 8f,
                center = Offset(point.x * size.width, point.y * size.height),
                style = Stroke(width = 3f),
            )
            drawCircle(
                color = Color(0xFF8AE8AA),
                radius = 4f,
                center = Offset(point.x * size.width, point.y * size.height),
            )
        }
    }
}

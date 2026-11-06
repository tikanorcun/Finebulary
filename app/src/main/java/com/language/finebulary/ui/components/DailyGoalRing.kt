package com.language.finebulary.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.language.finebulary.R

@Composable
fun DailyGoalRing(
    progress: Int,
    goal: Int,
    modifier: Modifier = Modifier
) {
    val isGoalReached = progress >= goal && goal > 0
    val sweepAngle = if (goal > 0) (progress.toFloat() / goal.toFloat()) * 360f else 0f
    val animatedSweepAngle by animateFloatAsState(
        targetValue = sweepAngle.coerceAtMost(360f),
        animationSpec = tween(durationMillis = 1000),
        label = "sweepAngle"
    )

    val primaryColor = if (isGoalReached) Color(0xFFFFD700) else MaterialTheme.colorScheme.primary 
    val secondaryColor = MaterialTheme.colorScheme.primaryContainer

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Background Circle
            drawArc(
                color = secondaryColor.copy(alpha = 0.2f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
            )

            // Progress Arc
            drawArc(
                brush = Brush.linearGradient(
                    colors = if (isGoalReached) {
                        listOf(Color(0xFFFFA000), Color(0xFFFFD700))
                    } else {
                        listOf(primaryColor.copy(alpha = 0.5f), primaryColor)
                    }
                ),
                startAngle = -90f,
                sweepAngle = animatedSweepAngle,
                useCenter = false,
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

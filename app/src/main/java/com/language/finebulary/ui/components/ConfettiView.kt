package com.language.finebulary.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

data class Particle(
    val x: Float,
    val y: Float,
    val color: Color,
    val radius: Float,
    val velocityX: Float,
    val velocityY: Float,
    val rotation: Float
)

@Composable
fun ConfettiView(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    val particles = remember {
        List(50) {
            Particle(
                x = Random.nextFloat(),
                y = Random.nextFloat() * -1f,
                color = listOf(Color.Yellow, Color.Cyan, Color(0xFFFF5722), Color(0xFF4CAF50), Color.Magenta).random(),
                radius = Random.nextFloat() * 10f + 5f,
                velocityX = (Random.nextFloat() - 0.5f) * 0.01f,
                velocityY = Random.nextFloat() * 0.02f + 0.01f,
                rotation = Random.nextFloat() * 360f
            )
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        particles.forEach { p ->
            val currentY = (p.y + (progress * p.velocityY * 100f)) % 1.2f
            val currentX = (p.x + (progress * p.velocityX * 100f)) % 1f
            
            if (currentY > 0 && currentY < 1) {
                drawCircle(
                    color = p.color,
                    radius = p.radius,
                    center = Offset(currentX * width, currentY * height)
                )
            }
        }
    }
}

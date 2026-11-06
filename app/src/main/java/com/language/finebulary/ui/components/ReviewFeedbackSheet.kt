package com.language.finebulary.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.language.finebulary.R
import com.language.finebulary.ui.viewmodel.ReviewResult
import com.language.finebulary.ui.viewmodel.ReviewStatus

@Composable
fun ReviewFeedbackSheet(
    result: ReviewResult?,
    onContinue: () -> Unit,
    onRetry: () -> Unit,
    questionKey: Int
) {
    // Better way to detect if we should use dark or light content colors
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    
    AnimatedVisibility(
        visible = result != null,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = Modifier.fillMaxSize()
    ) {
        if (result == null) return@AnimatedVisibility

        val (backgroundColor, contentColor) = when (result.status) {
            ReviewStatus.CORRECT -> {
                if (isDark) Color(0xFF1B3022) to Color(0xFF81C784)
                else Color(0xFFE8F5E9) to Color(0xFF1B5E20)
            }
            ReviewStatus.WRONG -> {
                if (isDark) Color(0xFF371E1E) to Color(0xFFE57373)
                else Color(0xFFFFEBEE) to Color(0xFFB71C1C)
            }
            ReviewStatus.CLOSE -> {
                if (isDark) Color(0xFF332B1A) to Color(0xFFFFD54F)
                else Color(0xFFFFF8E1) to Color(0xFFF57F17)
            }
        }

        val motivationalPhrases = listOf(
            "You did it!",
            "I knew you wouldn't turn me down!",
            "Keep it up!",
            "Fantastic job!",
            "You're a natural!",
            "Legendary work!",
            "Spot on!",
            "Brilliant recall!"
        )
        val phrase = remember(questionKey) { motivationalPhrases.random() }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 80.dp) // Give a bit more space for the sheet
                    .fillMaxHeight(), // Fill available space to ensure button is reachable
                shape = MaterialTheme.shapes.extraLarge.copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp)),
                color = backgroundColor,
                contentColor = contentColor, 
                tonalElevation = 8.dp,
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                        .padding(top = 24.dp, bottom = 0.dp) 
                    // Removed redundant navigationBarsPadding as it's handled by outer Scaffold
                ) {
                    // HEADER SECTION (Status + Motivational phrase)
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (result.status == ReviewStatus.WRONG) Icons.Default.Warning else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = when (result.status) {
                                    ReviewStatus.CORRECT -> stringResource(R.string.excellent)
                                    ReviewStatus.WRONG -> stringResource(R.string.take_a_look)
                                    ReviewStatus.CLOSE -> stringResource(R.string.so_close)
                                },
                                style = MaterialTheme.typography.headlineSmall,
                                color = contentColor,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (result.status == ReviewStatus.CORRECT) {
                            Text(
                                text = phrase,
                                style = MaterialTheme.typography.bodyMedium,
                                color = contentColor.copy(alpha = 0.8f),
                                modifier = Modifier.padding(start = 44.dp)
                            )
                        }

                        if (result.status == ReviewStatus.CLOSE) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.close_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = contentColor.copy(alpha = 0.8f),
                                modifier = Modifier.padding(start = 44.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // MIDDLE SECTION (Scrollable Word Details)
                    Box(modifier = Modifier.weight(1f)) {
                        WordFullDetailView(
                            word = result.word,
                            reviewState = null,
                            showStats = false,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // BOTTOM SECTION (Always visible button)
                    Button(
                        onClick = { if (result.status == ReviewStatus.CLOSE) onRetry() else onContinue() },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = contentColor, contentColor = backgroundColor),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = if (result.status == ReviewStatus.CLOSE) stringResource(R.string.try_again) else stringResource(R.string.continue_label),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

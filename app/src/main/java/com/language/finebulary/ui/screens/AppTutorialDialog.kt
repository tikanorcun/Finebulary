package com.language.finebulary.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.language.finebulary.R

data class TutorialStep(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun AppTutorialDialog(
    onFinish: () -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }

    val steps = listOf(
        TutorialStep(
            title = stringResource(R.string.tutorial_step1_title),
            description = stringResource(R.string.tutorial_step1_desc),
            icon = Icons.Default.FolderOpen,
            color = MaterialTheme.colorScheme.primary
        ),
        TutorialStep(
            title = stringResource(R.string.tutorial_step2_title),
            description = stringResource(R.string.tutorial_step2_desc),
            icon = Icons.Default.Psychology,
            color = MaterialTheme.colorScheme.secondary
        ),
        TutorialStep(
            title = stringResource(R.string.tutorial_step3_title),
            description = stringResource(R.string.tutorial_step3_desc),
            icon = Icons.AutoMirrored.Filled.MenuBook,
            color = MaterialTheme.colorScheme.tertiary
        ),
        TutorialStep(
            title = stringResource(R.string.tutorial_step4_title),
            description = stringResource(R.string.tutorial_step4_desc),
            icon = Icons.AutoMirrored.Filled.VolumeUp,
            color = Color(0xFF4CAF50)
        )
    )

    val step = steps[currentStep]

    AlertDialog(
        onDismissRequest = {},
        shape = MaterialTheme.shapes.extraLarge,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.tutorial_title_fmt, currentStep + 1, steps.size),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(onClick = onFinish) {
                    Text(stringResource(R.string.tutorial_skip), style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    modifier = Modifier.size(96.dp),
                    shape = CircleShape,
                    color = step.color.copy(alpha = 0.15f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = step.icon,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = step.color
                        )
                    }
                }

                Text(
                    text = step.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = step.description,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (currentStep < steps.size - 1) {
                        currentStep++
                    } else {
                        onFinish()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = if (currentStep < steps.size - 1) stringResource(R.string.tutorial_next) else stringResource(R.string.tutorial_start),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

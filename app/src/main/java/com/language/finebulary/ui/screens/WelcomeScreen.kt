package com.language.finebulary.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.language.finebulary.R
import com.language.finebulary.data.pref.ThemeMode
import com.language.finebulary.ui.components.DailyGoalRing
import com.language.finebulary.ui.components.ConfettiView
import com.language.finebulary.ui.components.getLanguageEmoji
import com.language.finebulary.ui.viewmodel.HomeViewModel
import com.language.finebulary.ui.viewmodel.TranslationState

@Composable
fun WelcomeScreen(
    viewModel: HomeViewModel,
    onNavigateToCourses: () -> Unit,
    onNavigateToMyWords: () -> Unit,
    onNavigateToIelts: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val todayProgress by viewModel.todayProgress.collectAsState()
    val dailyGoal by viewModel.dailyGoal.collectAsState()
    
    val importedCourses by viewModel.importedCourses.collectAsState()
    val personalFolders by viewModel.personalFolders.collectAsState()
    
    val totalWords = (importedCourses + personalFolders).sumOf { it.wordCount }
    val totalStreaks = (importedCourses + personalFolders).sumOf { it.course.currentStreak }

    val isGoalReached = todayProgress >= dailyGoal && dailyGoal > 0
    var showGoalPicker by remember { mutableStateOf(false) }

    if (showGoalPicker) {
        var customGoal by remember { mutableStateOf(dailyGoal.toString()) }
        AlertDialog(
            onDismissRequest = { showGoalPicker = false },
            title = { Text(stringResource(R.string.daily_goal_label), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.goal_description), style = MaterialTheme.typography.bodyMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GoalOptionCard("5", dailyGoal == 5, Modifier.weight(1f)) { viewModel.setDailyGoal(5); showGoalPicker = false }
                        GoalOptionCard("15", dailyGoal == 15, Modifier.weight(1f)) { viewModel.setDailyGoal(15); showGoalPicker = false }
                        GoalOptionCard("30", dailyGoal == 30, Modifier.weight(1f)) { viewModel.setDailyGoal(30); showGoalPicker = false }
                    }
                    
                    OutlinedTextField(
                        value = customGoal,
                        onValueChange = { if (it.all { char -> char.isDigit() }) customGoal = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.custom_goal)) },
                        suffix = { Text(stringResource(R.string.words_caps).lowercase()) },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        singleLine = true,
                        shape = MaterialTheme.shapes.small
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val goal = customGoal.toIntOrNull() ?: 10
                    if (goal > 0) {
                        viewModel.setDailyGoal(goal)
                        showGoalPicker = false
                    }
                }) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoalPicker = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isGoalReached) {
                ConfettiView(modifier = Modifier.fillMaxSize())
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Branding Header at the very top
                Spacer(modifier = Modifier.statusBarsPadding())
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(32.dp))

                // QUICK STATS RIBBON
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Daily Progress Stat
                        StatItem(
                            icon = Icons.Default.Adjust,
                            value = "$todayProgress / $dailyGoal",
                            label = stringResource(R.string.goal_reached_label),
                            color = if (isGoalReached) Color(0xFFFFD700) else MaterialTheme.colorScheme.primary,
                            onClick = { showGoalPicker = true }
                        )

                        VerticalDivider(modifier = Modifier.height(40.dp).alpha(0.3f))

                        // Streak Stat
                        StatItem(
                            icon = Icons.Default.Whatshot,
                            value = "$totalStreaks",
                            label = stringResource(R.string.streak_label),
                            color = Color(0xFFFF5722)
                        )

                        VerticalDivider(modifier = Modifier.height(40.dp).alpha(0.3f))

                        // Total Words Stat
                        StatItem(
                            icon = Icons.Default.AutoStories,
                            value = "$totalWords",
                            label = stringResource(R.string.vocabulary_label),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Hero Action Cards
                WelcomeHeroCard(
                    title = stringResource(R.string.courses),
                    subtitle = stringResource(R.string.courses_subtitle),
                    icon = Icons.AutoMirrored.Filled.List,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick = onNavigateToCourses
                )

                Spacer(modifier = Modifier.height(24.dp))

                WelcomeHeroCard(
                    title = stringResource(R.string.my_words),
                    subtitle = stringResource(R.string.my_words_subtitle),
                    icon = Icons.Default.Create,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    onClick = onNavigateToMyWords
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Special Mode Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally // Center aligned as requested
                ) {
                    Text(
                        text = stringResource(R.string.special_mode),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    WelcomeSpecialCard(
                        title = stringResource(R.string.ielts_academy),
                        subtitle = stringResource(R.string.ielts_academy_desc),
                        icon = Icons.Default.School,
                        onClick = onNavigateToIelts
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Bottom Settings
                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier
                        .size(60.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.settings),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StatItem(icon: ImageVector, value: String, label: String, color: Color, onClick: (() -> Unit)? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
    }
}

@Composable
fun GoalOptionCard(label: String, isSelected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier.height(60.dp),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else Color.Transparent
        )
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(label, fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeSpecialCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "special_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    OutlinedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeHeroCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    isWip: Boolean = false
) {
    ElevatedCard(
        onClick = { if (!isWip) onClick() },
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isWip) containerColor.copy(alpha = 0.5f) else containerColor,
            contentColor = if (isWip) contentColor.copy(alpha = 0.5f) else contentColor
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = if (isWip) 2.dp else 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                color = contentColor.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = contentColor
                    )
                }
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (isWip) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.tertiary,
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                text = stringResource(R.string.coming_soon),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor.copy(alpha = 0.7f)
                )
            }
            
            if (!isWip) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = contentColor.copy(alpha = 0.4f)
                )
            }
        }
    }
}
